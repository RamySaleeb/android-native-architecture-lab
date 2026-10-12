import { test } from 'node:test';
import assert from 'node:assert/strict';
import { PackModel, decode, storageKey } from './model.mjs';
function memory(initial = null) { let raw = initial; return {getItem: () => raw, setItem: (_, value) => { raw = value; }}; }
test('first visit starts empty; packing and unpacking survive recreation', () => {
  const storage = memory(); const model = new PackModel(storage);
  assert.equal(model.packed.size, 0); assert.equal(model.setPacked('bottle', true), true);
  assert.deepEqual([...new PackModel(storage).packed], ['bottle']);
  model.setPacked('bottle', false); assert.equal(new PackModel(storage).packed.size, 0);
});
test('filters show appropriate items without writing data', () => {
  const storage = memory('["map"]'); const model = new PackModel(storage);
  model.setFilter('Packed'); assert.deepEqual(model.visible.map(x => x.id), ['map']);
  model.setFilter('Remaining'); assert.equal(model.visible.length, 5);
  assert.equal(storage.getItem(storageKey), '["map"]');
});
test('one-shot simulated failure keeps durable state and permits retry', () => {
  const model = new PackModel(memory('["map"]')); model.armFailure();
  assert.equal(model.setPacked('bottle', true), false); assert.deepEqual([...model.packed], ['map']);
  assert.equal(model.failNext, false); assert.ok(model.error);
  assert.equal(model.setPacked('bottle', true), true); assert.equal(model.packed.size, 2); assert.equal(model.error, '');
});
test('storage write failure never changes displayed packed state', () => {
  const storage = memory('["map"]'); storage.setItem = () => { throw new Error('Quota'); };
  const model = new PackModel(storage); assert.equal(model.setPacked('map', false), false);
  assert.deepEqual([...model.packed], ['map']);
});
test('blocked read disables mutations and recovers on retry', () => {
  const storage = memory('["map"]'); const get = storage.getItem;
  storage.getItem = () => { throw new Error('Blocked'); };
  const model = new PackModel(storage); assert.equal(model.readFailed, true);
  assert.equal(model.setPacked('bottle', true), false);
  storage.getItem = get; assert.equal(model.load(), true); assert.equal(model.packed.has('map'), true);
});
test('malformed data is preserved instead of silently discarded', () => {
  const storage = memory('{broken'); const model = new PackModel(storage);
  assert.equal(model.readFailed, true); assert.equal(model.setPacked('map', true), false);
  assert.equal(storage.getItem(storageKey), '{broken');
  assert.throws(() => decode('[42]'));
});
test('unknown and duplicate saved IDs do not inflate progress', () => {
  assert.deepEqual([...decode('["map","obsolete","map"]')], ['map']);
  const model = new PackModel(memory()); assert.equal(model.setPacked('unknown', true), false);
});
test('a later tab mutation includes the newest stored selections', () => {
  const storage = memory(); const first = new PackModel(storage); const second = new PackModel(storage);
  first.setPacked('bottle', true); second.setPacked('map', true);
  assert.deepEqual([...second.packed], ['bottle', 'map']); first.load(); assert.equal(first.packed.size, 2);
});
