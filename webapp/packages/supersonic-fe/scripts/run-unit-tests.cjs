const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const projectRoot = path.resolve(__dirname, '..');
const outDir = path.join(projectRoot, '.tmp-unit');
const tscBin = path.join(projectRoot, 'node_modules', 'typescript', 'bin', 'tsc');
const jestBin = path.join(projectRoot, 'node_modules', '.bin', 'jest');

function compileMenuFilter() {
  fs.rmSync(outDir, { recursive: true, force: true });
  execFileSync(process.execPath, [
    tscBin,
    '-p',
    path.join(projectRoot, 'tsconfig.unit.json'),
  ], { stdio: 'inherit' });
}

function runMenuFilterTests() {
  assert.ok(fs.existsSync(jestBin), 'Local jest binary not found');
  execFileSync(jestBin, [
    '--config',
    path.join(projectRoot, 'jest.unit.config.cjs'),
    '--runInBand',
  ], {
    cwd: projectRoot,
    stdio: 'inherit',
  });
}

compileMenuFilter();
runMenuFilterTests();
