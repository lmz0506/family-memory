export function albumColumns(width:number){return Math.min(6,Math.max(1,Math.floor((Math.max(0,width)+24)/289)))}
export function albumPageSize(width:number){return albumColumns(width)*3}
export function mediaGridColumns(count:number){return count<=1?1:count===5?4:count===2||count===4?2:3}

export function mediaGridRows(count:number){return count<=2?1:count<=6?2:3}
