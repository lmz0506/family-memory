import type {UniverseBatch,UniverseItem} from '@/types'

type YearState = {afterId:number;complete:boolean;error:string}
type FetchBatch = (year:number,afterId:number,signal:AbortSignal)=>Promise<UniverseBatch>

/** 当前星海会话内的轻量索引缓存；串行分批拉取，浏览中的年份优先。 */
export class UniversePreloader {
  readonly items = new Map<number,UniverseItem>()
  private states = new Map<number,YearState>()
  private queue:number[] = []
  private running = false
  private priorityYear:number|undefined
  private disposed = false
  private abort = new AbortController()

  constructor(private fetchBatch:FetchBatch,private changed:()=>void,
    private yieldWork:()=>Promise<void> = ()=>new Promise(resolve=>setTimeout(resolve,32))) {}

  start(years:number[]) {
    this.priorityYear=years[0]
    for(const year of years)if(!this.states.has(year)){
      this.states.set(year,{afterId:0,complete:false,error:''});this.queue.push(year)
    }
    void this.run()
  }

  /** 调整下一批的优先级，不打断当前请求，不重复已完成年份。 */
  prioritize(year:number,retry=false) {
    const state=this.states.get(year)
    if(!state||state.complete||this.disposed)return
    if(state.error&&!retry)return
    this.priorityYear=year;state.error='';this.queue=this.queue.filter(value=>value!==year);this.queue.unshift(year)
    void this.run()
  }

  isComplete(year:number){return Boolean(this.states.get(year)?.complete)}
  errorFor(year:number){return this.states.get(year)?.error||''}
  forDate(date:string){return [...this.items.values()].filter(item=>item.nodeDate===date).sort((a,b)=>a.indexId-b.indexId)}

  dispose(){this.disposed=true;this.abort.abort();this.queue=[];this.items.clear();this.states.clear()}

  private async run() {
    if(this.running||this.disposed)return
    this.running=true
    try {
      while(this.queue.length&&!this.disposed){
        const year=this.queue.shift()!,state=this.states.get(year)!
        if(state.complete||state.error)continue
        try {
          const batch=await this.fetchBatch(year,state.afterId,this.abort.signal)
          if(this.disposed)return
          if(batch.year!==year || (batch.hasMore&&(!batch.items.length||batch.nextAfterId<=state.afterId)))throw Error('星点批次游标无效，请重新载入')
          for(const item of batch.items)this.items.set(item.indexId,item)
          state.afterId=batch.nextAfterId;state.complete=!batch.hasMore
          // 保持当前年份连续读取；若用户已优先选择其他年份，则让它先走。
          if(!state.complete&&!this.queue.includes(year)){
            if(this.priorityYear===year)this.queue.unshift(year)
            else this.queue.push(year)
          }
          this.changed()
        }catch(error){
          if(this.disposed)return
          state.error=(error as Error).message||'星点暂时无法载入';this.changed()
        }
        if(this.queue.length)await this.yieldWork()
      }
    }finally{this.running=false}
  }
}
