#!/usr/bin/env python3
key = "pqktageoufxxmaxj"
xor_key = 5
encrypted = ''.join(chr(ord(c) ^ xor_key) for c in key)
print(f"Original: {key}")
print(f"Encrypted: {encrypted}")
print(f"Length: {len(encrypted)}")

# Decrypt to verify
decrypted = ''.join(chr(ord(c) ^ xor_key) for c in encrypted)
print(f"Decrypted: {decrypted}")
