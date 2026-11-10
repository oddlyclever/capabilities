// Mocha's 2-second limit per test is too short for encoding an image in the browser on a slow machine: GitHub's
// hosted Mac, with three cores, timed the wasm tests out there while they pass in well under a second on a laptop.
config.client = config.client || {};
config.client.mocha = Object.assign(config.client.mocha || {}, { timeout: 30000 });
