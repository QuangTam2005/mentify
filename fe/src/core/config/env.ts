import { z } from 'zod';

const envSchema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().default(''),
  NEXT_PUBLIC_BACKEND_INTERNAL_URL: z.string().default('http://localhost:8080'),
  NEXT_PUBLIC_APP_ENV: z.enum(['local', 'development', 'staging', 'production']).default('local'),
  NEXT_PUBLIC_ENABLE_DEV_LOGIN: z.string().optional(),
  NEXT_PUBLIC_GOOGLE_AUTH_URL: z
    .string()
    .default('http://localhost:8080/oauth2/authorization/google'),
  NEXT_PUBLIC_DEFAULT_TIMEZONE: z.string().default('Asia/Ho_Chi_Minh'),
});

const parseEnv = () => {
  const envValues = {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
    NEXT_PUBLIC_BACKEND_INTERNAL_URL: process.env.NEXT_PUBLIC_BACKEND_INTERNAL_URL,
    NEXT_PUBLIC_APP_ENV: process.env.NEXT_PUBLIC_APP_ENV,
    NEXT_PUBLIC_ENABLE_DEV_LOGIN: process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN,
    NEXT_PUBLIC_GOOGLE_AUTH_URL: process.env.NEXT_PUBLIC_GOOGLE_AUTH_URL,
    NEXT_PUBLIC_DEFAULT_TIMEZONE: process.env.NEXT_PUBLIC_DEFAULT_TIMEZONE,
  };

  const parsed = envSchema.safeParse(envValues);

  if (!parsed.success) {
    // Không ném lỗi làm crash build thời điểm static generation nếu env bị thiếu, dùng fallback an toàn
    console.warn('⚠️ Invalid environment variables:', parsed.error.format());
    return envSchema.parse({});
  }

  return parsed.data;
};

export const env = parseEnv();

/**
 * Kiểm tra xem tính năng Developer Login có được phép hiển thị và sử dụng hay không.
 * Điều kiện bảo mật nghiêm ngặt (Strict Opt-In):
 * Bắt buộc thỏa mãn ĐỒNG THỜI 2 điều kiện:
 * 1. process.env.NODE_ENV === "development"
 * 2. process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN === "true"
 * Mặc định luôn tắt. Không cho phép bất kỳ fallback hay bypass qua APP_ENV.
 */
export const isDevLoginEnabled = (): boolean => {
  return (
    process.env.NODE_ENV === 'development' &&
    process.env.NEXT_PUBLIC_ENABLE_DEV_LOGIN === 'true'
  );
};
