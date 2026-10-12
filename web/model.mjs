export const gear = [
  { id: 'bottle', name: 'Water bottle', category: 'Essentials', detail: 'Refill before you leave', icon: '◒' },
  { id: 'map', name: 'Pocket map', category: 'Essentials', detail: 'A little room for a detour', icon: '◇' },
  { id: 'jacket', name: 'Light jacket', category: 'Clothing', detail: 'For the cooler part of the day', icon: '⌁' },
  { id: 'cap', name: 'Everyday cap', category: 'Clothing', detail: 'Keep it within reach', icon: '◓' },
  { id: 'camera', name: 'Compact camera', category: 'Extras', detail: 'Bring home a few good frames', icon: '◎' },
  { id: 'notebook', name: 'Field notebook', category: 'Extras', detail: 'Make space for an observation', icon: '▤' },
];
export const storageKey = 'ramysaleeb.trail-pack.web.v1';
const valid = new Set(gear.map(item => item.id));
export function decode(raw) {
  if (raw === null) return new Set();
  const data = JSON.parse(raw);
  if (!Array.isArray(data) || data.some(id => typeof id !== 'string')) throw new Error('Invalid checklist');
  return new Set(data.filter(id => valid.has(id)));
}
export class PackModel {
  constructor(storage) {
    this.storage = storage; this.packed = new Set(); this.filter = 'All';
    this.failNext = false; this.readFailed = false; this.error = ''; this.message = '';
    this.load();
  }
  load() {
    try {
      const packed = decode(this.storage.getItem(storageKey));
      this.packed = packed; this.readFailed = false; this.error = ''; this.message = 'Checklist loaded.';
      return true;
    } catch {
      this.readFailed = true;
      this.error = 'Could not read saved data. Enable browser storage, then retry. Existing data has not been overwritten.';
      return false;
    }
  }
  setFilter(filter) { if (['All', 'Remaining', 'Packed'].includes(filter)) this.filter = filter; }
  armFailure() { if (!this.readFailed) { this.failNext = true; this.error = ''; this.message = 'Failure armed. Try checking or unchecking an item.'; } }
  setPacked(id, checked) {
    if (this.readFailed || !valid.has(id)) return false;
    try {
      if (this.failNext) { this.failNext = false; throw new Error('Simulated failure'); }
      // Read the latest stored set before each synchronous write to avoid using stale tab state.
      const next = decode(this.storage.getItem(storageKey));
      checked ? next.add(id) : next.delete(id);
      this.storage.setItem(storageKey, JSON.stringify([...next]));
      this.packed = next; this.error = ''; this.message = 'Saved in this browser.';
      return true;
    } catch {
      this.error = 'Save failed. Your checklist is unchanged. Try the item again. If it keeps failing, check browser storage permissions.';
      return false;
    }
  }
  get visible() { return gear.filter(item => this.filter === 'All' || (this.filter === 'Packed') === this.packed.has(item.id)); }
}
