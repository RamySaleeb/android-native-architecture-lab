import { PackModel, gear, storageKey } from './model.mjs';
// Access through functions so blocked localStorage access is handled by the model.
const model = new PackModel({ getItem: key => window.localStorage.getItem(key), setItem: (key, value) => window.localStorage.setItem(key, value) });
const list = document.querySelector('#items');
for (const item of gear) {
  const row = document.createElement('li'); row.className = 'gear'; row.dataset.id = item.id;
  const label = document.createElement('label');
  const input = document.createElement('input'); input.type = 'checkbox'; input.id = item.id;
  input.setAttribute('aria-label', item.name);
  input.addEventListener('change', () => {
    model.setPacked(item.id, input.checked); render();
    if (row.hidden) {
      const next = [...list.querySelectorAll('li:not([hidden]) input')].find(el => !el.disabled);
      (next || document.querySelector(`[data-filter="${model.filter}"]`)).focus();
    }
  });
  const icon = document.createElement('span'); icon.className = 'symbol'; icon.textContent = item.icon; icon.setAttribute('aria-hidden', 'true');
  const words = document.createElement('span'); words.className = 'words';
  for (const [className, text] of [['category',item.category.toUpperCase()],['name',item.name],['detail',item.detail]]) {
    const span = document.createElement('span'); span.className = className; span.textContent = text; words.append(span);
  }
  label.append(input, icon, words); row.append(label); list.append(row);
}
function render() {
  const visible = new Set(model.visible.map(item => item.id));
  for (const row of list.children) {
    row.hidden = !visible.has(row.dataset.id); row.classList.toggle('packed',model.packed.has(row.dataset.id));
    const input = row.querySelector('input'); input.checked = model.packed.has(row.dataset.id); input.disabled = model.readFailed;
  }
  document.querySelectorAll('[data-filter]').forEach(button => button.setAttribute('aria-pressed', String(model.filter === button.dataset.filter)));
  const count = model.packed.size;
  document.querySelector('#count').textContent = String(count);
  const progress = document.querySelector('#progress'); progress.value = count; progress.textContent = `${count} of 6`;
  document.querySelector('#progress-note').textContent = count === 6 ? 'All packed. The day is yours.' : 'One small thing at a time.';
  const empty = document.querySelector('#empty'); empty.hidden = visible.size > 0;
  empty.textContent = model.filter === 'Packed' ? 'Nothing packed yet. Start with one small thing.' : 'All packed. Enjoy the day out.';
  const fail = document.querySelector('#fail'); fail.disabled = model.failNext || model.readFailed;
  fail.textContent = model.failNext ? 'Failure armed — tap an item' : 'Simulate next save failure ↗';
  const status = document.querySelector('#status'); status.textContent = model.error || `${model.message} ${count} of 6 packed.`;
  status.classList.toggle('error', Boolean(model.error));
  document.querySelector('#retry').hidden = !model.readFailed;
}
document.querySelectorAll('[data-filter]').forEach(button => button.addEventListener('click', () => {model.setFilter(button.dataset.filter);render();}));
document.querySelector('#fail').addEventListener('click', () => {model.armFailure();render();});
document.querySelector('#retry').addEventListener('click', () => {model.load();render();if(!model.readFailed) document.querySelector('[data-filter="All"]').focus();});
window.addEventListener('storage', event => {if(event.key === storageKey || event.key === null){model.load();render();}});
render();
