/* Lucian UX/UI Prompt #039 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#039 */
const cards = [...document.querySelectorAll('.story-card')];
const search = document.querySelector('#memory-search');
let expandedStories = false;
function filterCards() {
  const keyword = search.value.trim();
  let visible = 0;
  cards.forEach(card => {
    const matches = card.dataset.title.includes(keyword) && (card.dataset.kind !== 'note' || expandedStories || Boolean(keyword));
    card.hidden = !matches;
    card.style.display = matches ? 'block' : 'none';
    if (matches) visible++;
  });
  document.querySelector('.no-results').hidden = visible > 0;
}
search.addEventListener('input', filterCards);
document.addEventListener('keydown', event => {if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {event.preventDefault(); search.focus();}});
document.querySelectorAll('.like').forEach(button => button.addEventListener('click', () => {
  const liked = button.getAttribute('aria-pressed') === 'true';
  button.setAttribute('aria-pressed', String(!liked));
  const count = button.querySelector('span');
  count.textContent = String(Number(count.textContent) + (liked ? -1 : 1));
}));
document.querySelector('.theme-toggle').addEventListener('click', event => {
  const dark = document.documentElement.dataset.theme !== 'dark';
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
  event.currentTarget.setAttribute('aria-label', dark ? '切换浅色模式' : '切换深色模式');
  event.currentTarget.querySelector('use').setAttribute('href', dark ? '#sun' : '#moon');
});
const dialog = document.querySelector('#preview-dialog');
const content = document.querySelector('#dialog-content');
function openDialog(html, variant = '') {dialog.classList.toggle('compose-dialog', variant === 'compose');content.innerHTML = html; dialog.showModal(); document.body.style.overflow = 'hidden';}
dialog.addEventListener('close', () => {document.body.style.overflow = '';});
document.querySelector('.dialog-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => {if (event.target === dialog) {const box = dialog.getBoundingClientRect(); if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close();}});
const stories = {
  lake:['湖边的晚风','lake-sunset.jpg','绕了一点远路，却遇见了今年最温柔的晚霞。小满一路追着风跑，爸爸在后面喊着慢一点。回家的路有很多条，和家人一起走的那一条，总是最喜欢的。'],
  child:['又长大了一点点','child-growing.jpg','今天第一次自己系好了鞋带。认真了好久，然后抬头说：“爸爸，我会啦！”有些成长很小，小到只是一根鞋带；有些幸福很大，大到想一直记着。'],
  birthday:['愿你每一岁，都有爱相伴','birthday-cake.jpg','蜡烛亮起来的时候，你闭上眼睛许了一个很长很长的愿望。我们没有问是什么，只希望你的每一岁，都能被爱包围。生日快乐，我的小朋友。'],
  family:['有你们的地方，就是家','family-sunset.jpg','终于一起去看了海。夕阳落下来的时候，我们手牵着手，说好明年还要一起来。原来最好的风景，是身边的你们。'],
  note:['幸福的形状',null,'下班回家，门还没开，就听见小满喊妈妈。厨房里是爸爸煮的汤，阳台上是刚晒好的衣服。好像也没有什么特别的事。可我想，这就是幸福的样子吧。']
};
function storyPhotos(key) {
  return key === 'lake' ? ['lake-sunset.jpg','family-sunset.jpg','child-growing.jpg'] : key === 'child' ? ['child-growing.jpg','birthday-cake.jpg','family-sunset.jpg','lake-sunset.jpg'] : stories[key]?.[1] ? [stories[key][1]] : [];
}
const viewer = document.createElement('dialog');
viewer.className = 'photo-viewer';
viewer.setAttribute('aria-label', '照片大图预览');
viewer.innerHTML = '<header class="viewer-header"><div><b class="viewer-title"></b><span class="viewer-counter" aria-live="polite"></span></div><button class="viewer-close" aria-label="关闭照片预览"><svg class="icon"><use href="#close"/></svg></button></header><div class="viewer-stage"><button class="viewer-arrow viewer-prev" aria-label="上一张照片"><svg class="icon"><use href="#chevron"/></svg></button><img class="viewer-image" alt=""><button class="viewer-arrow viewer-next" aria-label="下一张照片"><svg class="icon"><use href="#chevron"/></svg></button></div><p class="viewer-caption">左右切换照片 · Esc 关闭</p><div class="viewer-thumbs" aria-label="选择照片"></div>';
document.body.appendChild(viewer);
let photoGroup = '', photoIndex = 0, photoOpener = null;
const viewerImage = viewer.querySelector('.viewer-image');
function showPhoto(index) {
  const photos = storyPhotos(photoGroup);
  photoIndex = (index + photos.length) % photos.length;
  viewerImage.src = '../assets/images/' + photos[photoIndex];
  viewerImage.alt = `${stories[photoGroup][0]} · 第${photoIndex + 1}张照片`;
  viewer.querySelector('.viewer-counter').textContent = `${photoIndex + 1} / ${photos.length}`;
  viewer.querySelectorAll('.viewer-thumb').forEach((thumb,i) => thumb.setAttribute('aria-pressed',String(i === photoIndex)));
}
function openPhotos(key, index, opener) {
  photoGroup = key;
  photoOpener = opener;
  const photos = storyPhotos(key);
  if (!photos.length) return;
  viewer.querySelector('.viewer-title').textContent = stories[key][0];
  viewer.querySelectorAll('.viewer-arrow').forEach(button => {button.hidden = photos.length < 2;});
  viewer.querySelector('.viewer-caption').textContent = photos.length > 1 ? '左右切换照片 · Esc 关闭' : 'Esc 或点击空白处关闭';
  viewer.querySelector('.viewer-thumbs').innerHTML = photos.map((photo,i) => `<button class="viewer-thumb" aria-label="查看第${i+1}张照片" aria-pressed="false" data-index="${i}"><img src="../assets/images/${photo}" alt=""></button>`).join('');
  showPhoto(index);
  viewer.showModal();
  document.body.style.overflow = 'hidden';
}
document.addEventListener('click', event => {
  const tile = event.target.closest('[data-photo-group]');
  if (tile) openPhotos(tile.dataset.photoGroup,Number(tile.dataset.photoIndex),tile);
});
viewer.querySelector('.viewer-close').addEventListener('click', () => viewer.close());
viewer.querySelector('.viewer-prev').addEventListener('click', () => showPhoto(photoIndex - 1));
viewer.querySelector('.viewer-next').addEventListener('click', () => showPhoto(photoIndex + 1));
viewer.querySelector('.viewer-thumbs').addEventListener('click', event => {
  const thumb = event.target.closest('[data-index]');
  if (thumb) showPhoto(Number(thumb.dataset.index));
});
viewer.addEventListener('keydown', event => {
  if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {event.preventDefault();showPhoto(photoIndex + (event.key === 'ArrowLeft' ? -1 : 1));}
});
const stage = viewer.querySelector('.viewer-stage');
stage.addEventListener('click', event => {if (event.target === stage) viewer.close();});
let touchOrigin = null;
stage.addEventListener('touchstart', event => {if(event.touches.length === 1) touchOrigin = {x:event.touches[0].clientX,y:event.touches[0].clientY};else touchOrigin = null;}, {passive:true});
stage.addEventListener('touchend', event => {
  if (!touchOrigin || !event.changedTouches[0]) return;
  const dx = event.changedTouches[0].clientX - touchOrigin.x, dy = event.changedTouches[0].clientY - touchOrigin.y;
  if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) showPhoto(photoIndex + (dx < 0 ? 1 : -1));
  touchOrigin = null;
}, {passive:true});
viewer.addEventListener('close', () => {document.body.style.overflow = dialog.open ? 'hidden' : '';photoOpener?.focus({preventScroll:true});});
document.querySelectorAll('[data-story]').forEach(button => button.addEventListener('click', () => {
  const [title, image, text] = stories[button.dataset.story];
  const photos = button.dataset.story === 'lake' ? ['lake-sunset.jpg','family-sunset.jpg','child-growing.jpg'] : button.dataset.story === 'child' ? ['child-growing.jpg','birthday-cake.jpg','family-sunset.jpg','lake-sunset.jpg'] : image ? [image] : [];
  const media = photos.length > 1 ? `<div class="dialog-gallery ${photos.length === 3 ? 'gallery-three' : ''}">${photos.map((photo,i) => `<button class="photo-tile" data-photo-group="${button.dataset.story}" data-photo-index="${i}" aria-label="预览第${i+1}张照片"><img src="../assets/images/${photo}" alt="${title} · 第${i+1}张照片"></button>`).join('')}</div>` : image ? `<button class="photo-tile" data-photo-group="${button.dataset.story}" data-photo-index="0" aria-label="预览照片"><img class="dialog-image" src="../assets/images/${image}" alt="${title}"></button>` : '';
  openDialog(`<span class="dialog-kicker">我们家的故事</span>${media}<h2 class="dialog-title">${title}</h2><p class="dialog-text">${text}</p><p class="dialog-hint">静态预览 · 宫格使用示例照片组合，未连接真实数据</p>`);
}));
document.querySelectorAll('.compose').forEach(button => button.addEventListener('click', () => {
  openDialog('<header class="compose-head"><div class="compose-eyebrow"><svg class="icon"><use href="#book"/></svg>林家小院<span>·</span>新的回忆</div><h2 id="compose-heading">记录这一刻</h2><p>不必写得特别，生活本身就值得。</p></header><form class="compose-form" aria-labelledby="compose-heading"><div class="compose-paper"><input class="compose-title" aria-label="回忆标题" required maxlength="80" placeholder="给今天起个名字…"><span class="compose-rule"></span><textarea class="compose-story" aria-label="回忆正文" required placeholder="发生了什么，让你想要记住这一刻？"></textarea></div><div class="compose-attachments"><button type="button" class="attach-button"><svg class="icon"><use href="#image"/></svg>添加照片</button><span>给故事添一点画面</span><input class="attachment-input" type="file" accept="image/*" multiple hidden aria-label="选择回忆照片"></div><div class="attachment-previews" aria-label="已选择的照片" hidden></div><div class="compose-private"><svg class="icon"><use href="#heart"/></svg>属于我们的小事，留给家人慢慢看。</div><footer class="compose-footer"><span>静态预览 · 不上传或保存</span><div><button type="button" class="compose-cancel">取消</button><button type="submit" class="primary">完成预览<svg class="icon"><use href="#arrow"/></svg></button></div></footer></form>', 'compose');
  const form = content.querySelector('form');
  form.addEventListener('submit', event => {event.preventDefault(); dialog.close();});
  content.querySelector('.compose-cancel').addEventListener('click', () => dialog.close());
  const fileInput = content.querySelector('.attachment-input');
  const previews = content.querySelector('.attachment-previews');
  const objectUrls = [];
  content.querySelector('.attach-button').addEventListener('click', () => fileInput.click());
  fileInput.addEventListener('change', () => {
    [...fileInput.files].filter(file => file.type.startsWith('image/')).slice(0,9-objectUrls.length).forEach(file => {
      const url = URL.createObjectURL(file);objectUrls.push(url);
      const image = document.createElement('img');image.src = url;image.alt = file.name;previews.appendChild(image);
    });
    previews.hidden = !objectUrls.length;
    fileInput.value = '';
  });
  dialog.addEventListener('close', () => objectUrls.forEach(url => URL.revokeObjectURL(url)), {once:true});
}));
document.querySelectorAll('[data-info]').forEach(button => button.addEventListener('click', () => {
  openDialog(`<span class="dialog-kicker">拾光 · 设计预览</span><h2 class="dialog-title">${button.dataset.info}</h2><p class="dialog-text">这次先看看首页的视觉方向。这里的入口会在正式页面中接入对应功能。</p><p class="dialog-hint">当前为独立静态页面，不会改动现有系统。</p>`);
}));
document.querySelector('#more').addEventListener('click', () => {
  search.value = ''; expandedStories = true; filterCards();
  const note = document.querySelector('.note-card');
  note.hidden = false; note.style.display = 'block';
  document.querySelector('#more').textContent = '今天的示例故事，就先翻到这里。';
});


