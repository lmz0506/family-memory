export const fragmentPageSize=6
export function fragmentPageWindow(total:number,requested:number,size=fragmentPageSize){
 const pages=Math.ceil(total/size),page=Math.max(0,Math.min(requested,Math.max(0,pages-1)))
 return {pages,page,start:page*size,end:Math.min(total,(page+1)*size)}
}
