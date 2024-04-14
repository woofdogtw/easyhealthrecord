'use strict';

const crypto = require('crypto');

const aesKey = crypto.randomBytes(8).toString('hex');
const aesIv = crypto.randomBytes(8).toString('hex');

/**
 * To encrypt string to AES-256-CBC encrypted hexadecimal string.
 *
 * @param {string} data The raw string data to be encrypted.
 * @returns The encrypted hexadecimal string.
 */
function encrypt(data) {
  const cipher = crypto.createCipheriv('aes-128-cbc', aesKey, aesIv);
  return Buffer.from(
    cipher.update(data, 'utf8', 'hex') + cipher.final('hex')
  ).toString();
}

console.log(`KEY=${aesKey}`);
console.log(`IV=${aesIv}`);
console.log(`DATA=${process.argv[2]}`);
for (let i = 2; i < process.argv.length; i++) {
  console.log(`RESULT=${encrypt(process.argv[i])}`);
}
