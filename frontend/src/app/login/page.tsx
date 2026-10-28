import { Suspense } from "react";
import { LoginForm } from "@/features/auth/LoginForm";

export default function LoginPage() {
  // useSearchParams cần nằm trong Suspense khi build tĩnh.
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
