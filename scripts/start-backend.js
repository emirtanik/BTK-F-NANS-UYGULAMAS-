const { spawn } = require('node:child_process');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const isWin = process.platform === 'win32';
const mvn = path.join(root, isWin ? 'mvnw.cmd' : 'mvnw');

const child = spawn(mvn, ['spring-boot:run'], {
  cwd: root,
  stdio: 'inherit',
  shell: isWin,
});

child.on('exit', (code) => process.exit(code ?? 0));
