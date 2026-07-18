import { NextResponse } from "next/server";

export async function GET(req: Request) {
  try {
    const authHeader = req.headers.get("authorization");
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

    const res = await fetch(`${apiUrl}/customers/export`, {
      method: "GET",
      headers: authHeader ? { Authorization: authHeader } : {},
    });

    if (!res.ok) {
      return NextResponse.json({ error: "Failed to fetch CSV from backend" }, { status: res.status });
    }

    const blob = await res.blob();
    return new NextResponse(blob, {
      status: 200,
      headers: {
        "Content-Type": "text/csv",
        "Content-Disposition": 'attachment; filename="customers.csv"',
      },
    });
  } catch (error) {
    console.error("CSV Export error proxy:", error);
    return NextResponse.json({ error: "Internal Server Error" }, { status: 500 });
  }
}
