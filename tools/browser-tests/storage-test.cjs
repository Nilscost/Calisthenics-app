const { chromium, webkit } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('node:fs');
const base = process.env.STORAGE_TEST_URL || 'http://127.0.0.1:4822/';
const results = [];

async function run(engine, browserType) {
  const browser = await browserType.launch({ headless: true });
  const context = await browser.newContext();
  const errors = [];
  context.on('page', page => {
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    page.on('pageerror', error => errors.push(error.message));
  });
  async function check(mode, id, page) {
    page ||= await context.newPage();
    await page.goto(`${base}?mode=${mode}&id=${id}`);
    await page.waitForFunction(() => ['passed', 'failed'].includes(document.body.dataset.status), null, { timeout: 30000 });
    const status = await page.locator('body').getAttribute('data-status');
    const detail = await page.locator('body').innerText();
    if (status !== 'passed') throw new Error(`${engine} ${mode}: ${detail}`);
    results.push({ engine, mode, status });
    return page;
  }
  try {
    const prefix = `${engine}-${Date.now()}`;
    await check('contract', `${prefix}-contract`);
    const persisted = `${prefix}-persisted`;
    const first = await check('prepare', persisted);
    await first.reload();
    await first.waitForFunction(() => document.body.dataset.status === 'failed');
    // Reloading the prepare command must reject its duplicate primary key.
    const duplicate = JSON.parse(await first.locator('body').innerText());
    if (duplicate.error !== 'duplicate session') throw new Error(`${engine} unexpected reload failure: ${duplicate.error}`);
    results.push({ engine, mode: 'duplicateAfterReload', status: 'passed' });
    const second = await context.newPage();
    await Promise.all([check('revise', persisted, first), check('revise', persisted, second)]);
    await check('verify', persisted, first);
    await check('abort', persisted, first);
    for (const mode of ['corrupt', 'missing', 'version']) await check(mode, `${prefix}-${mode}`);
    const settingsId = `${prefix}-settings`;
    await check('settingsPrepare', settingsId, first);
    await check('settingsVerify', settingsId, first);
    await first.reload();
    await first.waitForFunction(() => ['passed', 'failed'].includes(document.body.dataset.status));
    if (await first.locator('body').getAttribute('data-status') !== 'passed') throw new Error(await first.locator('body').innerText());
    results.push({ engine, mode: 'settingsReload', status: 'passed' });
    await check('settingsAtomic', settingsId, first);
    if (errors.length) throw new Error(`${engine} browser errors: ${errors.join('\n')}`);
  } finally { await context.close(); await browser.close(); }
}

(async () => {
  const deadline = Date.now() + 10000;
  for (;;) {
    try { const response = await fetch(base); if (response.ok) break; } catch (_) { }
    if (Date.now() >= deadline) throw new Error(`Storage test server did not become ready: ${base}`);
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  await run('chromium', chromium);
  await run('webkit', webkit);
  const output = JSON.stringify({ results }, null, 2);
  if (process.env.STORAGE_TEST_RESULTS) fs.writeFileSync(process.env.STORAGE_TEST_RESULTS, output + '\n');
  console.log(output);
})().catch(error => { console.error(error); process.exitCode = 1; });
