const views=new Map<string,unknown>()
export function readViewState<T>(key:string){return views.get(key) as T|undefined}
export function writeViewState<T>(key:string,value:T){views.set(key,value);if(views.size>30)views.delete(views.keys().next().value!)}
export function invalidateViewData(prefix:string){for(const [key,value] of views)if(key.startsWith(prefix)&&value&&typeof value==='object')views.set(key,{...value,stale:true})}
export function clearViewStates(){views.clear()}
