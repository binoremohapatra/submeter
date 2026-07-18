import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";
    
    const res = await fetch(`${apiUrl}/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (res.ok) {
      return NextResponse.json({ success: true });
    } else {
      let errorText = await res.text();
      try {
        const jsonError = JSON.parse(errorText);
        errorText = jsonError.message || errorText;
      } catch (e) {
        // Not JSON, keep text
      }
      return NextResponse.json({ error: errorText }, { status: res.status });
    }
  } catch (error) {
    console.error("Signup error proxy:", error);
    return NextResponse.json({ error: "Internal Server Error" }, { status: 500 });
  }
}
