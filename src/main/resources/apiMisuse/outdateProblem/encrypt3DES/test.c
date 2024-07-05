#include <windows.h>
#include <wincrypt.h>

void advapi() {
  HCRYPTPROV hCryptProv;
  HCRYPTKEY hKey;
  HCRYPTHASH hHash;

  // 不推荐使用 3DES
  CryptDeriveKey(hCryptProv, "CALG_3DES", hHash, 0, &hKey);

  // 推荐
  // CryptDeriveKey(hCryptProv, "CALG_AES_256", hHash, 0, &hKey);
}