export const overviewMagnification=1.25
export const calendarMagnification={year:1.15,month:1.25,day:1.35,memory:1,capsule:1}
// Content click should approach from the current distance and never back away.
export function contentCameraDistance(current:number,fitted:number){
 return Math.min(current,Math.max(70,current*.82,fitted))
}
