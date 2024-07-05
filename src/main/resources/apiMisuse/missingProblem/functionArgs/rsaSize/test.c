#include <openssl/rsa.h>
#include <openssl/evp.h>

void encrypt_with_openssl(EVP_PKEY_CTX *ctx) {

  // 密钥长度过短
  EVP_PKEY_CTX_set_rsa_keygen_bits(ctx, 1024);
}