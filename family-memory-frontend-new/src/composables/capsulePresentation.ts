export function capsuleReadPhase(opened:boolean,seen:boolean,reduced:boolean):'locked'|'ready'|'read'{return !opened?'locked':seen||reduced?'read':'ready'}
export function canReadCapsule(opened:boolean,phase:string){return opened&&phase==='read'}
