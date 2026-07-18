"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";
import { useRouter } from "next/navigation";
import { toast } from "@/components/ui/toast";
import Link from "next/link";

export default function RegisterPage() {
  const router = useRouter();
  const [orgName, setOrgName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFieldError(null);

    if (!orgName.trim()) {
      setFieldError("Organization Name is required");
      return;
    }
    if (!email.trim()) {
      setFieldError("Email is required");
      return;
    }
    if (!password) {
      setFieldError("Password is required");
      return;
    }
    if (password.length < 8) {
      setFieldError("Password must be at least 8 characters");
      return;
    }

    setLoading(true);

    try {
      // 1. Create user account
      const registerRes = await fetch("/api/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          orgName: orgName.trim(),
          email: email.trim(),
          password
        }),
      });

      if (!registerRes.ok) {
        let errorMessage = "Failed to register";
        try {
          const errData = await registerRes.json();
          errorMessage = errData.error || errData.message || errorMessage;
        } catch {
          errorMessage = await registerRes.text().catch(() => errorMessage);
        }
        setFieldError(errorMessage);
        toast.error("Registration failed");
        setLoading(false);
        return;
      }

      // 2. Sign in the new user automatically
      const res = await signIn("credentials", {
        email: email.trim(),
        password,
        redirect: false,
      });

      if (res?.error) {
        setFieldError("Account created, but sign-in failed. Please try logging in manually.");
        setLoading(false);
      } else {
        toast.success("Account created successfully");
        router.push("/dashboard");
      }
    } catch (err: any) {
      setFieldError("Network error during registration");
      toast.error("Network error");
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "var(--gray-950)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <div style={{ width: "100%", maxWidth: 400 }}>
        {/* Wordmark */}
        <div
          style={{
            marginBottom: 32,
          }}
        >
          <Link href="/"
            style={{
              display: "block",
              fontSize: "var(--text-base)",
              fontWeight: 500,
              color: "var(--gray-100)",
              letterSpacing: "-0.01em",
              marginBottom: 8,
              textDecoration: "none",
            }}
          >
            &gt; SubMeter
          </Link>
          <h1
            style={{
              fontSize: "var(--text-xl)",
              fontWeight: 500,
              color: "var(--gray-100)",
              margin: 0,
              lineHeight: 1.3,
            }}
          >
            Create your account
          </h1>
          <p
            style={{
              fontSize: "var(--text-sm)",
              color: "var(--gray-500)",
              margin: "4px 0 0",
            }}
          >
            Start metering your subscriptions in minutes
          </p>
        </div>

        {/* Form card */}
        <form
          onSubmit={handleSubmit}
          noValidate
          style={{
            background: "var(--gray-800)",
            border: "1px solid var(--gray-700)",
            borderRadius: "var(--radius-lg)",
            padding: 24,
          }}
        >
          {fieldError && (
            <div
              role="alert"
              style={{
                marginBottom: 16,
                padding: "10px 12px",
                background: "rgba(239,68,68,0.08)",
                border: "1px solid rgba(239,68,68,0.25)",
                borderRadius: "var(--radius-md)",
                fontSize: "var(--text-sm)",
                color: "var(--danger)",
                lineHeight: 1.4,
              }}
            >
              {fieldError}
            </div>
          )}

          {/* Org Name */}
          <div style={{ marginBottom: 16 }}>
            <label
              htmlFor="orgName"
              style={{
                display: "block",
                fontSize: "var(--text-xs)",
                fontWeight: 500,
                color: "var(--gray-500)",
                textTransform: "uppercase",
                letterSpacing: "0.08em",
                marginBottom: 6,
              }}
            >
              Organization Name
            </label>
            <input
              id="orgName"
              type="text"
              value={orgName}
              onChange={(e) => setOrgName(e.target.value)}
              placeholder="Acme Corp"
              required
              disabled={loading}
              style={{
                width: "100%",
                background: "var(--gray-900)",
                border: "1px solid var(--gray-700)",
                borderRadius: "var(--radius-md)",
                padding: "10px 12px",
                fontSize: "var(--text-base)",
                color: "var(--gray-100)",
                outline: "none",
                transition: "border-color 150ms cubic-bezier(0.4,0,0.2,1)",
                opacity: loading ? 0.5 : 1,
              }}
              onFocus={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--accent)")
              }
              onBlur={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--gray-700)")
              }
            />
          </div>

          {/* Email */}
          <div style={{ marginBottom: 16 }}>
            <label
              htmlFor="email"
              style={{
                display: "block",
                fontSize: "var(--text-xs)",
                fontWeight: 500,
                color: "var(--gray-500)",
                textTransform: "uppercase",
                letterSpacing: "0.08em",
                marginBottom: 6,
              }}
            >
              Work email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              required
              disabled={loading}
              style={{
                width: "100%",
                background: "var(--gray-900)",
                border: "1px solid var(--gray-700)",
                borderRadius: "var(--radius-md)",
                padding: "10px 12px",
                fontSize: "var(--text-base)",
                color: "var(--gray-100)",
                outline: "none",
                transition: "border-color 150ms cubic-bezier(0.4,0,0.2,1)",
                opacity: loading ? 0.5 : 1,
              }}
              onFocus={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--accent)")
              }
              onBlur={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--gray-700)")
              }
            />
          </div>

          {/* Password */}
          <div style={{ marginBottom: 24 }}>
            <label
              htmlFor="password"
              style={{
                display: "block",
                fontSize: "var(--text-xs)",
                fontWeight: 500,
                color: "var(--gray-500)",
                textTransform: "uppercase",
                letterSpacing: "0.08em",
                marginBottom: 6,
              }}
            >
              Password (Min 8 chars)
            </label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
              style={{
                width: "100%",
                background: "var(--gray-900)",
                border: "1px solid var(--gray-700)",
                borderRadius: "var(--radius-md)",
                padding: "10px 12px",
                fontSize: "var(--text-base)",
                color: "var(--gray-100)",
                outline: "none",
                transition: "border-color 150ms cubic-bezier(0.4,0,0.2,1)",
                opacity: loading ? 0.5 : 1,
              }}
              onFocus={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--accent)")
              }
              onBlur={(e) =>
              ((e.currentTarget as HTMLInputElement).style.borderColor =
                "var(--gray-700)")
              }
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: "100%",
              background: loading ? "var(--gray-700)" : "#ffffff",
              border: "none",
              borderRadius: "var(--radius-md)",
              padding: "10px 16px",
              fontSize: "var(--text-base)",
              fontWeight: 600,
              color: loading ? "var(--gray-500)" : "#000000",
              cursor: loading ? "not-allowed" : "pointer",
              transition: "all 150ms cubic-bezier(0.4,0,0.2,1)",
              minHeight: 44,
              boxShadow: loading ? "none" : "0 0 25px rgba(255,255,255,0.6), inset 0 0 15px rgba(255,255,255,0.2)",
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                (e.currentTarget as HTMLButtonElement).style.opacity = "0.95";
                (e.currentTarget as HTMLButtonElement).style.boxShadow = "0 0 40px rgba(255,255,255,0.9), inset 0 0 15px rgba(255,255,255,0.3), 0 0 60px rgba(255,255,255,0.5)";
              }
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLButtonElement).style.opacity = "1";
              (e.currentTarget as HTMLButtonElement).style.boxShadow = "0 0 25px rgba(255,255,255,0.6), inset 0 0 15px rgba(255,255,255,0.2)";
            }}
            onMouseDown={(e) => {
              if (!loading) {
                (e.currentTarget as HTMLButtonElement).style.opacity = "0.85";
              }
            }}
            onMouseUp={(e) => {
              if (!loading) {
                (e.currentTarget as HTMLButtonElement).style.opacity = "0.95";
              }
            }}
          >
            {loading ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p
          style={{
            marginTop: 16,
            textAlign: "center",
            fontSize: "var(--text-sm)",
            color: "var(--gray-500)",
          }}
        >
          Already have an account?{" "}
          <Link
            href="/login"
            style={{
              color: "var(--neutral-100)",
              textDecoration: "underline",
              fontWeight: 500,
            }}
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
