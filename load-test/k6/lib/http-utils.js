/**
 * k6 시나리오 공통 유틸리티
 */

/**
 * 난수 ID 생성 (VU 간 충돌 방지)
 * @param {number} length - 생성 길이 (기본값: 8)
 * @returns {string} 영숫자 ID
 */
export function generateId(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  const array = new Uint32Array(length);
  crypto.getRandomValues(array);
  for (let i = 0; i < length; i++) {
    result += chars[array[i] % chars.length];
  }
  return result;
}

/**
 * 가명 생성 (VU별 익명 사용자 식별자)
 * @param {number} vu - 현재 VU 번호
 * @param {number} iter - 현재 이터레이션 번호
 * @returns {string} 형식: user_{vu}_{iter}
 */
export function getName(vu, iter) {
  return `user_${vu}_${iter}`;
}
