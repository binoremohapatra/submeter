"use client";

import { useSession } from "next-auth/react";
import { motion } from "framer-motion";
import { Building2, Key, Trash2, Mail, ShieldAlert, Check, Copy } from "lucide-react";
import { useState, useEffect, useRef } from "react";
import { apiFetch } from "@/lib/api";

function SettingSection({ title, desc, children }: { title: string; desc: string; children: React.ReactNode }) {
  return (
    <div style={{
      display: "grid", gridTemplateColumns: "240px 1fr", gap: 40,
      padding: "32px 0", borderBottom: "1px solid var(--neutral-800)",
    }}
      className="flex-col-mobile"
    >
      <div>
        <h3 style={{ fontSize: 14, fontWeight: 500, color: "var(--neutral-50)", marginBottom: 6 }}>{title}</h3>
        <p style={{ fontSize: 13, color: "var(--neutral-500)", lineHeight: 1.5 }}>{desc}</p>
      </div>
      <div>
        {children}
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const { data: session } = useSession();

  const [org, setOrg] = useState<any>(null);
  const [apiKeys, setApiKeys] = useState<any[]>([]);
  const [members, setMembers] = useState<any[]>([]);
  const [invites, setInvites] = useState<any[]>([]);
  const [newPlaintextKey, setNewPlaintextKey] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved">("idle");
  const [copyState, setCopyState] = useState(false);
  const [inviteState, setInviteState] = useState<"idle" | "input" | "sending" | "sent">("idle");
  const [inviteEmail, setInviteEmail] = useState("");
  const [deleteState, setDeleteState] = useState(false);
  const [logoState, setLogoState] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLogoState(true);
    const formData = new FormData();
    formData.append("file", file);

    try {
      const updatedOrg = await apiFetch("/v1/organizations/settings/logo", session, {
        method: "POST",
        body: formData,
      });
      setOrg(updatedOrg);
    } catch (err: any) {
      alert("Error uploading logo: " + err.message);
    } finally {
      setLogoState(false);
    }
  };

  // Form states
  const [orgName, setOrgName] = useState("");
  const [supportEmail, setSupportEmail] = useState("");
  const [timezone, setTimezone] = useState("UTC");
  const [currency, setCurrency] = useState("USD");

  useEffect(() => {
    if (!session) return;
    Promise.all([
      apiFetch<any>("/v1/organizations/settings", session).catch(() => null),
      apiFetch<any>("/v1/api-keys", session).catch(() => []),
      apiFetch<any>("/v1/team/members", session).catch(() => []),
      apiFetch<any>("/v1/team/invites", session).catch(() => [])
    ]).then(([orgData, keysData, membersData, invitesData]) => {
      if (orgData) {
        setOrg(orgData);
        setOrgName(orgData.name || "");
        setSupportEmail(orgData.supportEmail || "");
        setTimezone(orgData.timezone || "UTC");
        setCurrency(orgData.currency || "USD");
        if (typeof window !== "undefined") localStorage.setItem("currency_code", orgData.currency || "USD");
      }
      setApiKeys(Array.isArray(keysData) ? keysData : []);
      setMembers(Array.isArray(membersData) ? membersData : []);
      setInvites(Array.isArray(invitesData) ? invitesData : []);
      setLoading(false);
    }).catch(e => {
      console.error(e);
      setApiKeys([]);
      setMembers([]);
      setInvites([]);
      setLoading(false);
    });
  }, []);

  const handleSave = async () => {
    setSaveState("saving");
    try {
      await apiFetch("/v1/organizations/settings", session, {
        method: "PUT",
        body: JSON.stringify({
          name: orgName,
          supportEmail,
          timezone,
          currency
        })
      });
      if (typeof window !== "undefined") localStorage.setItem("currency_code", currency);
      setSaveState("saved");
      setTimeout(() => setSaveState("idle"), 2000);
    } catch (e: any) {
      setSaveState("idle");
      alert("Failed to save settings: " + e.message);
    }
  };


  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopyState(true);
    setTimeout(() => setCopyState(false), 2000);
  };

  const handleRollKey = async () => {
    if (!confirm("Are you sure? Old keys will need to be revoked manually.")) return;
    try {
      const data: any = await apiFetch("/v1/api-keys", session, {
        method: "POST",
        body: JSON.stringify({ name: "Demo API Key", scopes: [], environment: "TEST" })
      });

      setApiKeys([...apiKeys, data.entity]);
      setNewPlaintextKey(data.plaintextKey);
    } catch (e: any) {
      console.error("Exception during generate key:", e);
      alert(e.message || "An unexpected error occurred");
    }
  };

  const handleRevokeKey = async (id: string) => {
    if (!confirm("Revoke this key immediately? All integrations using it will break.")) return;
    try {
      await apiFetch(`/v1/api-keys/${id}/revoke`, session, { method: "POST" });
      setApiKeys(apiKeys.map(k => k.id === id ? { ...k, revokedAt: new Date().toISOString() } : k));
    } catch (e: any) {
      alert("Failed to revoke: " + e.message);
    }
  };

  const handleInviteSend = async () => {
    if (!inviteEmail) return;
    setInviteState("sending");
    try {
      const data: any = await apiFetch("/v1/team/invites", session, {
        method: "POST",
        body: JSON.stringify({ email: inviteEmail, role: "MEMBER" })
      });
      setInvites([...invites, data.entity]);
      setInviteState("sent");
      setInviteEmail("");
      setTimeout(() => {
        setInviteState("idle");
      }, 2500);
      alert(`Invitation sent! Link (for demo): /accept-invite?token=${data.token}`);
    } catch (e: any) {
      alert("Failed to invite: " + e.message);
      setInviteState("idle");
    }
  };
  const handleRemoveMember = async (id: string) => {
    if (!confirm("Remove this member?")) return;
    try {
      await apiFetch(`/v1/team/members/${id}`, session, { method: "DELETE" });
      setMembers(members.filter(m => m.id !== id));
    } catch (e: any) {
      alert("Failed to remove member: " + e.message);
    }
  };

  if (loading) {
    return <div style={{ color: "var(--neutral-400)" }}>Loading settings...</div>;
  }

  return (
    <div style={{ maxWidth: 800 }}>
      <div style={{ marginBottom: 8 }}>
        <h1 style={{ fontSize: 20, fontWeight: 500, letterSpacing: "-0.02em", color: "var(--neutral-50)" }}>
          Settings
        </h1>
        <p style={{ fontSize: 13, color: "var(--neutral-500)", marginTop: 3 }}>
          Manage your organization and preferences
        </p>
      </div>

      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}>
        <SettingSection
          title="Organization Profile"
          desc="This is your company's information. It will appear on your invoices and receipts."
        >
          <div style={{ display: "flex", flexDirection: "column", gap: 20, maxWidth: 400 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
              <div style={{
                width: 64, height: 64, borderRadius: 8,
                background: "var(--neutral-800)", border: "1px solid var(--neutral-700)",
                display: "flex", alignItems: "center", justifyContent: "center",
                color: "var(--neutral-400)",
                overflow: "hidden"
              }}>
                {org?.logoUrl ? (
                  <img src={org.logoUrl} alt="Logo" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                ) : (
                  <Building2 style={{ width: 24, height: 24 }} />
                )}
              </div>
              <input type="file" ref={fileInputRef} hidden accept="image/*" onChange={handleLogoUpload} />
              <button className="btn btn-secondary btn-sm" disabled={logoState} onClick={() => fileInputRef.current?.click()}>
                {logoState ? "Uploading..." : "Upload Logo"}
              </button>
            </div>

            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 500, color: "var(--neutral-400)", marginBottom: 6 }}>Organization Name</label>
              <input className="input" value={orgName} onChange={e => setOrgName(e.target.value)} />
            </div>

            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 500, color: "var(--neutral-400)", marginBottom: 6 }}>Support Email</label>
              <input className="input" type="email" value={supportEmail} onChange={e => setSupportEmail(e.target.value)} />
            </div>

            <div style={{ display: "flex", gap: 16 }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 12, fontWeight: 500, color: "var(--neutral-400)", marginBottom: 6 }}>Timezone</label>
                <select className="input" value={timezone} onChange={e => setTimezone(e.target.value)}>
                  <option value="UTC">UTC</option>
                  <option value="America/New_York">Eastern Time (US)</option>
                  <option value="America/Los_Angeles">Pacific Time (US)</option>
                  <option value="Asia/Kolkata">India Standard Time</option>
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 12, fontWeight: 500, color: "var(--neutral-400)", marginBottom: 6 }}>Currency</label>
                <select className="input" value={currency} onChange={e => setCurrency(e.target.value)}>
                  <option value="USD">USD ($)</option>
                  <option value="EUR">EUR (€)</option>
                  <option value="GBP">GBP (£)</option>
                  <option value="INR">INR (₹)</option>
                </select>
                <p style={{ fontSize: 11, color: "var(--neutral-600)", marginTop: 6, lineHeight: 1.5 }}>
                  Controls the currency symbol shown on invoices and receipts.
                </p>
              </div>
            </div>

            <button
              className="btn btn-primary"
              style={{ width: "fit-content", marginTop: 8, display: "flex", alignItems: "center", gap: 6, opacity: saveState === "saving" ? 0.7 : 1 }}
              onClick={handleSave}
              disabled={saveState !== "idle"}
            >
              {saveState === "saving" ? "Saving..." : saveState === "saved" ? <><Check style={{ width: 14, height: 14 }} /> Saved!</> : "Save Changes"}
            </button>
          </div>
        </SettingSection>

        <SettingSection
          title="API Keys"
          desc="Generate demo API keys (sk_test_) to explore the SubMeter API. These are test keys only."
        >
          {newPlaintextKey && (
            <div style={{
              background: "var(--green-soft)", border: "1px solid var(--green)",
              borderRadius: 8, padding: 16, marginBottom: 20
            }}>
              <h4 style={{ color: "var(--green)", fontSize: 14, fontWeight: 500, marginBottom: 8 }}>Your new API key</h4>
              <p style={{ color: "var(--green)", fontSize: 13, marginBottom: 12, opacity: 0.9 }}>
                Please copy this key now. For security reasons, you will not be able to see it again.
              </p>
              <div style={{ display: "flex", gap: 8 }}>
                <input className="input" value={newPlaintextKey} readOnly style={{ fontFamily: "var(--font-mono)", color: "var(--green)", flex: 1, borderColor: "var(--green)", background: "rgba(0,0,0,0.1)" }} />
                <button className="btn btn-primary" onClick={() => handleCopy(newPlaintextKey)}>
                  {copyState ? <Check style={{ width: 14, height: 14 }} /> : "Copy Key"}
                </button>
              </div>
            </div>
          )}

          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {apiKeys.map(key => (
              <div key={key.id} style={{
                background: "var(--neutral-900)", border: "1px solid var(--neutral-800)",
                borderRadius: 8, padding: 16, opacity: key.revokedAt ? 0.5 : 1
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 12 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <Key style={{ width: 14, height: 14, color: "var(--neutral-400)" }} />
                    <span style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-200)" }}>{key.name}</span>
                    {key.revokedAt && <span style={{ fontSize: 11, padding: "2px 6px", background: "var(--red-soft)", color: "var(--red)", borderRadius: 4 }}>Revoked</span>}
                  </div>
                  <span style={{ fontSize: 11, color: "var(--neutral-500)", fontFamily: "var(--font-mono)" }}>
                    {new Date(key.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
                  <div style={{ fontFamily: "var(--font-mono)", color: "var(--neutral-400)", fontSize: 14 }}>
                    {key.prefix}••••••••••••••••••••••••{key.last4}
                  </div>
                  <div style={{ flex: 1 }} />
                  {!key.revokedAt && (
                    <button className="btn btn-secondary btn-sm" onClick={() => handleRevokeKey(key.id)} style={{ color: "var(--red)" }}>
                      Revoke
                    </button>
                  )}
                </div>
              </div>
            ))}

            {apiKeys.length === 0 && (
              <div style={{ fontSize: 13, color: "var(--neutral-500)" }}>No API keys generated yet.</div>
            )}
          </div>

          <button className="btn btn-secondary" style={{ marginTop: 16 }} onClick={handleRollKey}>
            Generate New Key
          </button>
        </SettingSection>

        <SettingSection
          title="Team Members"
          desc="Manage who has access to your billing infrastructure."
        >
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {/* Active Members */}
            {members.map(member => (
              <div key={member.id} style={{
                background: "var(--neutral-900)", border: "1px solid var(--neutral-800)",
                borderRadius: 8, overflow: "hidden",
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div style={{ width: 32, height: 32, borderRadius: 16, background: "var(--neutral-800)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 500, color: "var(--neutral-300)" }}>
                      {member.user?.email?.[0]?.toUpperCase() ?? "U"}
                    </div>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-100)" }}>{member.user?.email}</div>
                      <div style={{ fontSize: 12, color: "var(--neutral-500)" }}>Joined {new Date(member.joinedAt).toLocaleDateString()}</div>
                    </div>
                  </div>
                  <span style={{ fontSize: 11, padding: "2px 8px", borderRadius: 4, background: "var(--neutral-800)", color: "var(--neutral-300)", fontWeight: 500 }}>
                    {member.role}
                  </span>
                </div>
              </div>
            ))}

            {/* Pending Invites */}
            {invites.filter(i => i.status === "PENDING").map(invite => (
              <div key={invite.id} style={{
                background: "transparent", border: "1px dashed var(--neutral-700)",
                borderRadius: 8, overflow: "hidden", opacity: 0.8
              }}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 16px" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <div style={{ width: 32, height: 32, borderRadius: 16, background: "transparent", border: "1px dashed var(--neutral-600)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 500, color: "var(--neutral-500)" }}>
                      <Mail style={{ width: 14, height: 14 }} />
                    </div>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 500, color: "var(--neutral-300)" }}>{invite.email}</div>
                      <div style={{ fontSize: 12, color: "var(--neutral-500)" }}>Invitation Pending</div>
                    </div>
                  </div>
                  <span style={{ fontSize: 11, padding: "2px 8px", borderRadius: 4, background: "var(--neutral-800)", color: "var(--neutral-400)", fontWeight: 500 }}>
                    {invite.role}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {inviteState === "idle" && (
            <button className="btn btn-secondary" style={{ marginTop: 16, display: "flex", alignItems: "center", gap: 6 }} onClick={() => setInviteState("input")}>
              <Mail style={{ width: 14, height: 14 }} /> Invite Member
            </button>
          )}

          {inviteState !== "idle" && (
            <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} style={{ overflow: "hidden", marginTop: 16 }}>
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <input className="input" type="email" placeholder="colleague@example.com" value={inviteEmail} onChange={e => setInviteEmail(e.target.value)} style={{ maxWidth: 300 }} autoFocus />
                <button className="btn btn-primary" onClick={handleInviteSend} disabled={inviteState !== "input" || !inviteEmail}>
                  {inviteState === "sending" ? "Sending..." : inviteState === "sent" ? <><Check style={{ width: 14, height: 14 }} /> Sent!</> : "Send Invite"}
                </button>
                {inviteState === "input" && (
                  <button className="btn btn-secondary" onClick={() => setInviteState("idle")}>Cancel</button>
                )}
              </div>
            </motion.div>
          )}
        </SettingSection>

        <SettingSection
          title="Danger Zone"
          desc="Irreversible, destructive actions."
        >
          <div style={{
            background: "var(--red-soft)", border: "1px solid var(--red-ring)",
            borderRadius: 8, padding: 20,
          }}>
            <div style={{ display: "flex", alignItems: "flex-start", gap: 12 }}>
              <ShieldAlert style={{ width: 18, height: 18, color: "var(--red)", marginTop: 2 }} />
              <div>
                <h4 style={{ fontSize: 14, fontWeight: 500, color: "var(--red)", marginBottom: 4 }}>Delete Organization</h4>
                <p style={{ fontSize: 13, color: "var(--red)", opacity: 0.8, marginBottom: 16, lineHeight: 1.5 }}>
                  This will immediately cancel all active subscriptions, delete all customer data, and remove your account. This action cannot be undone.
                </p>
                <button className="btn btn-danger" onClick={() => {
                  setDeleteState(true);
                  setTimeout(() => setDeleteState(false), 3000);
                }}>
                  {deleteState ? "Action restricted in demo mode." : <><Trash2 style={{ width: 14, height: 14 }} /> Delete Organization</>}
                </button>
              </div>
            </div>
          </div>
        </SettingSection>
      </motion.div>
    </div>
  );
}
