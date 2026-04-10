"use client";

import { useEffect, useState, useCallback } from "react";
import {
  LineChart as LineChartIcon,
  Cloud,
  ChevronDown,
  TrendingUp,
  TrendingDown,
  Server,
  Sparkles,
  AlertCircle,
  RefreshCw,
  CheckCircle2,
  DollarSign,
  Activity,
  Database,
} from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";
import api from "@/lib/api";

// ── Types ─────────────────────────────────────────────────────────────────────

interface Credential {
  credentialId: string; // FIXED: Aligned with Java Backend
  credentialLabel: string;
  provider: "AWS" | "AZURE";
}

interface DailySpend {
  date: string;
  cost: number;
}

interface ServiceCost {
  name: string;
  cost: number;
}

interface ResourceRow {
  resourceId: string;
  name: string;
  type: string;
  location: string;
  sku: string | null;
  estimatedMonthlyCost: number | null;
}

interface FinOpsData {
  provider: string;
  credentialId: string;
  totalMonthlyCost: number;
  projectedMonthlyCost: number;
  activeResourceCount: number;
  optimizationSavings: number;
  dailySpend: DailySpend[];
  costByService: ServiceCost[];
  resources: ResourceRow[];
  pricingCurrency: string;
  generatedAt: string;
  pricingDisclaimer: string;
}

// ── Constants ─────────────────────────────────────────────────────────────────

const PIE_COLORS = [
  "#0ea5e9", // sky-500
  "#8b5cf6", // violet-500
  "#06b6d4", // cyan-500
  "#f59e0b", // amber-500
  "#10b981", // emerald-500
  "#f43f5e", // rose-500
  "#a78bfa", // violet-400
  "#34d399", // emerald-400
];

const AZURE_RESOURCE_TYPES: Record<string, string> = {
  "microsoft.compute/virtualmachines": "Virtual Machine",
  "microsoft.storage/storageaccounts": "Storage Account",
  "microsoft.web/sites": "App Service",
  "microsoft.sql/servers": "SQL Server",
  "microsoft.network/publicipaddresses": "Public IP",
  "microsoft.network/virtualnetworks": "Virtual Network",
  "microsoft.containerservice/managedclusters": "AKS Cluster",
  "microsoft.keyvault/vaults": "Key Vault",
};

function formatResourceType(raw: string): string {
  return AZURE_RESOURCE_TYPES[raw?.toLowerCase()] ?? raw ?? "Unknown";
}

// ── Mock data fallback (used when API returns sparse data) ────────────────────

function buildFallbackDailySpend(): DailySpend[] {
  return Array.from({ length: 30 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (29 - i));
    return {
      date: d.toLocaleDateString("en-US", { month: "short", day: "numeric" }),
      cost: 0,
    };
  });
}

// ── Formatters ────────────────────────────────────────────────────────────────

function fmt(value: number | null | undefined): string {
  if (value == null) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function fmtDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

// ── Credential Select ─────────────────────────────────────────────────────────

function CredentialSelect({
  credentials,
  selected,
  onSelect,
  disabled,
}: {
  credentials: Credential[];
  selected: string;
  onSelect: (id: string) => void;
  disabled: boolean;
}) {
  const [open, setOpen] = useState(false);
  const selectedCred = credentials.find((c) => c.credentialId === selected);

  useEffect(() => {
    function handler(e: MouseEvent) {
      const target = e.target as HTMLElement;
      if (!target.closest("[data-cred-select]")) setOpen(false);
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div data-cred-select className="relative w-80">
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((p) => !p)}
        className={`
          w-full flex items-center justify-between gap-3 px-4 py-2.5
          rounded-xl border text-sm text-left
          bg-slate-900/70 backdrop-blur-sm border-slate-700/60 text-slate-200
          hover:border-slate-600 hover:bg-slate-900
          focus:outline-none focus:ring-1 focus:ring-sky-500/50
          transition-all duration-200
          disabled:opacity-50 disabled:cursor-not-allowed
          ${open ? "border-sky-500/40 ring-1 ring-sky-500/25" : ""}
        `}
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="w-6 h-6 rounded-lg bg-sky-500/15 border border-sky-500/30 flex items-center justify-center flex-shrink-0">
            <Cloud className="w-3 h-3 text-sky-400" />
          </div>
          {selectedCred ? (
            <span className="truncate font-medium text-slate-200">
              {selectedCred.credentialLabel}
            </span>
          ) : (
            <span className="text-slate-500 font-mono text-xs tracking-wider">
              SELECT AZURE CREDENTIAL
            </span>
          )}
        </div>
        <ChevronDown
          className={`w-4 h-4 text-slate-500 flex-shrink-0 transition-transform duration-200
            ${open ? "rotate-180 text-sky-400" : ""}`}
        />
      </button>

      {open && (
        <div className="absolute z-50 w-full mt-2 py-1.5 rounded-xl border bg-slate-900/95 backdrop-blur-xl border-slate-700/60 shadow-2xl shadow-black/60">
          {credentials.length === 0 ? (
            <div className="px-4 py-3 text-xs text-slate-500 font-mono">
              No Azure credentials found
            </div>
          ) : (
            credentials.map((c) => (
              <button
                key={c.credentialId}
                type="button"
                onClick={() => {
                  onSelect(c.credentialId);
                  setOpen(false);
                }}
                className={`w-full flex items-center gap-2.5 px-4 py-2.5 text-sm text-left
                  hover:bg-sky-500/10 transition-colors duration-150
                  ${selected === c.credentialId ? "text-sky-300" : "text-slate-300"}`}
              >
                <Cloud className="w-3.5 h-3.5 text-slate-500 flex-shrink-0" />
                <span className="truncate">{c.credentialLabel}</span>
                {selected === c.credentialId && (
                  <CheckCircle2 className="w-3.5 h-3.5 ml-auto text-sky-400 flex-shrink-0" />
                )}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

// ── KPI Card ──────────────────────────────────────────────────────────────────

function KpiCard({
  label,
  value,
  sub,
  icon: Icon,
  accent,
  trend,
}: {
  label: string;
  value: string;
  sub?: string;
  icon: React.ElementType;
  accent: "sky" | "violet" | "emerald" | "amber";
  trend?: "up" | "down" | "neutral";
}) {
  const accents = {
    sky: {
      icon: "bg-sky-500/15 border-sky-500/30 text-sky-400",
      value: "text-sky-300",
      glow: "shadow-sky-500/10",
    },
    violet: {
      icon: "bg-violet-500/15 border-violet-500/30 text-violet-400",
      value: "text-violet-300",
      glow: "shadow-violet-500/10",
    },
    emerald: {
      icon: "bg-emerald-500/15 border-emerald-500/30 text-emerald-400",
      value: "text-emerald-300",
      glow: "shadow-emerald-500/10",
    },
    amber: {
      icon: "bg-amber-500/15 border-amber-500/30 text-amber-400",
      value: "text-amber-300",
      glow: "shadow-amber-500/10",
    },
  };
  const a = accents[accent];

  return (
    <div
      className={`
        relative rounded-2xl border border-slate-700/50
        bg-slate-900/50 backdrop-blur-sm p-5
        shadow-lg ${a.glow}
        hover:border-slate-600/70 hover:bg-slate-900/70
        transition-all duration-300 group overflow-hidden
      `}
    >
      <div
        className={`absolute -top-6 -right-6 w-20 h-20 rounded-full opacity-20 blur-2xl
          ${accent === "sky" ? "bg-sky-500" : accent === "violet" ? "bg-violet-500" : accent === "emerald" ? "bg-emerald-500" : "bg-amber-500"}`}
      />

      <div className="relative flex items-start justify-between mb-3">
        <p className="text-xs font-mono text-slate-500 uppercase tracking-widest">
          {label}
        </p>
        <div
          className={`w-7 h-7 rounded-lg border flex items-center justify-center flex-shrink-0 ${a.icon}`}
        >
          <Icon className="w-3.5 h-3.5" />
        </div>
      </div>

      <p className={`text-2xl font-bold tracking-tight ${a.value}`}>{value}</p>

      {sub && (
        <div className="flex items-center gap-1 mt-1.5">
          {trend === "up" && <TrendingUp className="w-3 h-3 text-rose-400" />}
          {trend === "down" && (
            <TrendingDown className="w-3 h-3 text-emerald-400" />
          )}
          <p className="text-xs text-slate-500 font-mono">{sub}</p>
        </div>
      )}
    </div>
  );
}

// ── Skeleton Loader ───────────────────────────────────────────────────────────

function SkeletonLoader() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div
            key={i}
            className="h-28 rounded-2xl bg-slate-800/60 border border-slate-700/40"
          />
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 h-72 rounded-2xl bg-slate-800/60 border border-slate-700/40" />
        <div className="h-72 rounded-2xl bg-slate-800/60 border border-slate-700/40" />
      </div>
      <div className="h-64 rounded-2xl bg-slate-800/60 border border-slate-700/40" />
    </div>
  );
}

// ── Empty / No-credential state ───────────────────────────────────────────────

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
      <div className="w-14 h-14 rounded-2xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center">
        <Cloud className="w-6 h-6 text-slate-600" />
      </div>
      <div>
        <p className="text-slate-400 font-semibold">{message}</p>
        <p className="text-slate-600 text-sm mt-1 font-mono">
          Add Azure credentials in the Vault to begin
        </p>
      </div>
    </div>
  );
}

// ── Custom Tooltip ────────────────────────────────────────────────────────────

function AreaTooltip({
  active,
  payload,
  label,
}: {
  active?: boolean;
  payload?: { value: number }[];
  label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-slate-700/60 bg-slate-900/95 backdrop-blur-sm px-3 py-2 shadow-xl">
      <p className="text-xs font-mono text-slate-400 mb-1">{label}</p>
      <p className="text-sm font-bold text-sky-300">{fmt(payload[0]?.value)}</p>
    </div>
  );
}

function PieTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { name: string; value: number }[];
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-slate-700/60 bg-slate-900/95 backdrop-blur-sm px-3 py-2 shadow-xl">
      <p className="text-xs font-mono text-slate-400 mb-1">
        {payload[0]?.name}
      </p>
      <p className="text-sm font-bold text-white">{fmt(payload[0]?.value)}</p>
    </div>
  );
}

// ── Resource Table ────────────────────────────────────────────────────────────

function ResourceTable({ resources }: { resources: ResourceRow[] }) {
  const sorted = [...resources]
    .sort((a, b) => (b.estimatedMonthlyCost ?? 0) - (a.estimatedMonthlyCost ?? 0))
    .slice(0, 10);

  if (sorted.length === 0) {
    return (
      <div className="py-10 text-center text-slate-600 text-sm font-mono">
        No resources found for this credential
      </div>
    );
  }

  const maxCost = Math.max(...sorted.map((r) => r.estimatedMonthlyCost ?? 0), 1);

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-800/80">
            {["Resource Name", "Type", "Location", "Monthly Cost"].map((h) => (
              <th
                key={h}
                className="text-left px-4 py-3 text-xs font-mono text-slate-500 uppercase tracking-widest font-normal"
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map((r, i) => {
            const cost = r.estimatedMonthlyCost ?? 0;
            const barWidth = maxCost > 0 ? (cost / maxCost) * 100 : 0;
            return (
              <tr
                key={r.resourceId ?? i}
                className="border-b border-slate-800/40 hover:bg-slate-800/30 transition-colors duration-150 group"
              >
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-6 h-6 rounded-lg bg-slate-800 border border-slate-700/60 flex items-center justify-center flex-shrink-0">
                      <Server className="w-3 h-3 text-slate-500" />
                    </div>
                    <span className="text-slate-200 font-medium font-mono text-xs truncate max-w-[200px]">
                      {r.name ?? "—"}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <span className="px-2 py-0.5 rounded-md bg-slate-800/80 border border-slate-700/40 text-slate-400 text-xs font-mono">
                    {formatResourceType(r.type)}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className="text-slate-400 font-mono text-xs">
                    {r.location ?? "—"}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span
                      className={`font-bold text-sm font-mono ${
                        cost > 100
                          ? "text-rose-300"
                          : cost > 30
                            ? "text-amber-300"
                            : "text-emerald-300"
                      }`}
                    >
                      {r.estimatedMonthlyCost != null ? fmt(cost) : "—"}
                    </span>
                    {cost > 0 && (
                      <div className="flex-1 h-1 rounded-full bg-slate-800 overflow-hidden min-w-[60px] max-w-[100px]">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-sky-500 to-violet-500 transition-all duration-700"
                          style={{ width: `${barWidth}%` }}
                        />
                      </div>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function FinOpsPage() {
  const [credentials, setCredentials] = useState<Credential[]>([]);
  const [selectedId, setSelectedId] = useState<string>("");
  const [credLoading, setCredLoading] = useState(true);
  const [dataLoading, setDataLoading] = useState(false);
  const [finOpsData, setFinOpsData] = useState<FinOpsData | null>(null);
  const [error, setError] = useState<string>("");
  const [lastRefreshed, setLastRefreshed] = useState<Date | null>(null);

  useEffect(() => {
    async function loadCreds() {
      setCredLoading(true);
      try {
        // FIXED: Removed the /api/v1 prefix to prevent 404s
        const res = await api.get<{ credentials: Credential[] }>("/credentials");
        const azure = (res.data.credentials ?? []).filter(
          (c) => c.provider === "AZURE"
        );
        setCredentials(azure);
        if (azure.length === 1) setSelectedId(azure[0].credentialId);
      } catch {
        setCredentials([]);
      } finally {
        setCredLoading(false);
      }
    }
    loadCreds();
  }, []);

  const fetchFinOps = useCallback(async (credId: string) => {
    if (!credId) return;
    setDataLoading(true);
    setError("");
    setFinOpsData(null);
    try {
      // FIXED: Removed the /api/v1 prefix to prevent 404s
      const res = await api.get<FinOpsData>(
        `/finops/azure/dashboard?credentialId=${credId}`
      );
      setFinOpsData(res.data);
      setLastRefreshed(new Date());
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ??
        "Failed to load FinOps data. Please check your credential and try again.";
      setError(msg);
    } finally {
      setDataLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedId) fetchFinOps(selectedId);
  }, [selectedId, fetchFinOps]);

  const dailySpend: DailySpend[] =
    finOpsData?.dailySpend?.length
      ? finOpsData.dailySpend
      : buildFallbackDailySpend();

  const costByService: ServiceCost[] =
    finOpsData?.costByService?.length
      ? finOpsData.costByService
      : finOpsData?.resources?.length
        ? Object.entries(
            finOpsData.resources.reduce<Record<string, number>>((acc, r) => {
              const key = formatResourceType(r.type);
              acc[key] = (acc[key] ?? 0) + (r.estimatedMonthlyCost ?? 0);
              return acc;
            }, {})
          )
            .map(([name, cost]) => ({ name, cost }))
            .filter((s) => s.cost > 0)
            .sort((a, b) => b.cost - a.cost)
            .slice(0, 8)
        : [];

  const totalCost = finOpsData?.totalMonthlyCost ?? 0;
  const projectedCost = finOpsData?.projectedMonthlyCost ?? totalCost * 1.12;
  const activeResources =
    finOpsData?.activeResourceCount ?? finOpsData?.resources?.length ?? 0;
  const savings = finOpsData?.optimizationSavings ?? -(totalCost * 0.08);

  const noAzureCreds = !credLoading && credentials.length === 0;
  const hasData = !!finOpsData && !dataLoading;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Compiler-Safe Custom Styles */}
      <style dangerouslySetInnerHTML={{
        __html: `
          @keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
          .animate-fade-in { animation: fadeIn 0.4s ease-out forwards; }
        `
      }} />

      <div
        className="fixed inset-0 opacity-[0.015] pointer-events-none"
        style={{
          backgroundImage:
            "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
        }}
      />
      <div className="fixed top-0 right-1/4 w-[600px] h-[400px] bg-sky-500/5 rounded-full blur-3xl pointer-events-none" />
      <div className="fixed bottom-0 left-1/4 w-[400px] h-[300px] bg-violet-500/4 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-7xl mx-auto px-6 py-10 space-y-8">

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-sky-500/15 border border-sky-500/30 flex items-center justify-center">
                <LineChartIcon className="w-5 h-5 text-sky-400" />
              </div>
              <div>
                <h1 className="text-2xl font-bold tracking-tight text-white">
                  Azure FinOps Console
                </h1>
                <p className="text-slate-500 text-sm">
                  Cloud cost intelligence and optimization.
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {lastRefreshed && (
              <p className="text-xs font-mono text-slate-600 hidden sm:block">
                Updated {fmtDate(lastRefreshed.toISOString())}
              </p>
            )}
            {selectedId && (
              <button
                onClick={() => fetchFinOps(selectedId)}
                disabled={dataLoading}
                className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-mono
                  text-slate-400 hover:text-slate-200 bg-slate-900/60 hover:bg-slate-800
                  border border-slate-700/60 hover:border-slate-600
                  transition-all duration-150 disabled:opacity-50"
              >
                <RefreshCw
                  className={`w-3.5 h-3.5 ${dataLoading ? "animate-spin" : ""}`}
                />
                Refresh
              </button>
            )}
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 p-4 rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm">
          <div className="flex items-center gap-2 flex-shrink-0">
            <Database className="w-4 h-4 text-slate-500" />
            <span className="text-xs font-mono text-slate-500 uppercase tracking-widest">
              Data Source
            </span>
          </div>

          {credLoading ? (
            <div className="w-80 h-10 rounded-xl bg-slate-800/60 animate-pulse" />
          ) : (
            <CredentialSelect
              credentials={credentials}
              selected={selectedId}
              onSelect={setSelectedId}
              disabled={dataLoading}
            />
          )}

          {finOpsData && (
            <div className="flex items-center gap-2 ml-auto">
              <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-xs font-mono text-emerald-400/80 uppercase tracking-wider">
                Live
              </span>
            </div>
          )}
        </div>

        {noAzureCreds && (
          <EmptyState message="No Azure credentials configured" />
        )}

        {!noAzureCreds && !selectedId && !credLoading && (
          <EmptyState message="Select an Azure credential to load cost data" />
        )}

        {error && !dataLoading && (
          <div className="flex items-start gap-4 p-4 rounded-2xl border border-rose-500/25 bg-rose-500/8">
            <div className="w-8 h-8 rounded-xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center flex-shrink-0">
              <AlertCircle className="w-4 h-4 text-rose-400" />
            </div>
            <div>
              <p className="text-rose-300 font-semibold text-sm">
                Failed to Load Data
              </p>
              <p className="text-slate-400 text-xs mt-0.5 font-mono">{error}</p>
            </div>
            <button
              onClick={() => fetchFinOps(selectedId)}
              className="ml-auto flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
                text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700
                border border-slate-700/60 transition-all duration-150 flex-shrink-0"
            >
              <RefreshCw className="w-3 h-3" />
              Retry
            </button>
          </div>
        )}

        {dataLoading && <SkeletonLoader />}

        {hasData && (
          <div className="space-y-6 animate-fade-in">

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <KpiCard
                label="Current Month Spend"
                value={fmt(totalCost)}
                sub={`${activeResources} resources tracked`}
                icon={DollarSign}
                accent="sky"
                trend="neutral"
              />
              <KpiCard
                label="Projected Spend"
                value={fmt(projectedCost)}
                sub="End-of-month forecast"
                icon={TrendingUp}
                accent="violet"
                trend="up"
              />
              <KpiCard
                label="Active Resources"
                value={String(activeResources)}
                sub="MindOps-managed"
                icon={Activity}
                accent="amber"
                trend="neutral"
              />
              <KpiCard
                label="Optimization Savings"
                value={fmt(Math.abs(savings))}
                sub="Potential monthly savings"
                icon={Sparkles}
                accent="emerald"
                trend="down"
              />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

              <div className="lg:col-span-2 rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm p-6">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <p className="text-sm font-semibold text-slate-200">
                      Cost Trend
                    </p>
                    <p className="text-xs text-slate-500 font-mono mt-0.5">
                      Daily spend · 30-day window
                    </p>
                  </div>
                  <div className="flex items-center gap-2 px-2.5 py-1 rounded-lg bg-sky-500/10 border border-sky-500/20">
                    <div className="w-2 h-0.5 rounded-full bg-sky-400" />
                    <span className="text-xs font-mono text-sky-400">USD/day</span>
                  </div>
                </div>

                <svg width="0" height="0" className="absolute">
                  <defs>
                    <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#0ea5e9" stopOpacity={0.35} />
                      <stop offset="75%" stopColor="#0ea5e9" stopOpacity={0.04} />
                      <stop offset="100%" stopColor="#0ea5e9" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                </svg>

                <ResponsiveContainer width="100%" height={240}>
                  <AreaChart
                    data={dailySpend}
                    margin={{ top: 4, right: 8, left: 0, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#0ea5e9" stopOpacity={0.3} />
                        <stop offset="100%" stopColor="#0ea5e9" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="#1e293b"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="date"
                      tick={{ fill: "#475569", fontSize: 10, fontFamily: "monospace" }}
                      tickLine={false}
                      axisLine={false}
                      interval={4}
                    />
                    <YAxis
                      tick={{ fill: "#475569", fontSize: 10, fontFamily: "monospace" }}
                      tickLine={false}
                      axisLine={false}
                      tickFormatter={(v: number) =>
                        v === 0 ? "$0" : `$${v.toFixed(0)}`
                      }
                      width={48}
                    />
                    <Tooltip content={<AreaTooltip />} />
                    <Area
                      type="monotone"
                      dataKey="cost"
                      stroke="#0ea5e9"
                      strokeWidth={2}
                      fill="url(#areaFill)"
                      dot={false}
                      activeDot={{
                        r: 4,
                        fill: "#0ea5e9",
                        stroke: "#0f172a",
                        strokeWidth: 2,
                      }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm p-6">
                <div className="mb-6">
                  <p className="text-sm font-semibold text-slate-200">
                    Cost by Service
                  </p>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">
                    Resource type distribution
                  </p>
                </div>

                {costByService.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-48 gap-3">
                    <Cloud className="w-8 h-8 text-slate-700" />
                    <p className="text-xs text-slate-600 font-mono text-center">
                      No service cost data available
                    </p>
                  </div>
                ) : (
                  <div className="flex flex-col items-center">
                    <ResponsiveContainer width="100%" height={180}>
                      <PieChart>
                        <Pie
                          data={costByService}
                          cx="50%"
                          cy="50%"
                          innerRadius={50}
                          outerRadius={80}
                          paddingAngle={3}
                          dataKey="cost"
                          nameKey="name"
                          strokeWidth={0}
                        >
                          {costByService.map((_, i) => (
                            <Cell
                              key={i}
                              fill={PIE_COLORS[i % PIE_COLORS.length]}
                              opacity={0.9}
                            />
                          ))}
                        </Pie>
                        <Tooltip content={<PieTooltip />} />
                      </PieChart>
                    </ResponsiveContainer>

                    <div className="-mt-2 text-center">
                      <p className="text-xs font-mono text-slate-500">Total</p>
                      <p className="text-sm font-bold text-sky-300">
                        {fmt(totalCost)}
                      </p>
                    </div>

                    <div className="w-full mt-4 space-y-1.5">
                      {costByService.slice(0, 6).map((s, i) => (
                        <div
                          key={i}
                          className="flex items-center justify-between gap-2"
                        >
                          <div className="flex items-center gap-2 min-w-0">
                            <div
                              className="w-2 h-2 rounded-full flex-shrink-0"
                              style={{
                                background: PIE_COLORS[i % PIE_COLORS.length],
                              }}
                            />
                            <span className="text-xs text-slate-400 font-mono truncate">
                              {s.name}
                            </span>
                          </div>
                          <span className="text-xs font-mono text-slate-300 flex-shrink-0">
                            {fmt(s.cost)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden">
              <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/60">
                <div>
                  <p className="text-sm font-semibold text-slate-200">
                    Top Cost Drivers
                  </p>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">
                    MindOps-managed resources · sorted by monthly spend
                  </p>
                </div>
                <div className="px-2.5 py-1 rounded-lg bg-slate-800/80 border border-slate-700/40">
                  <span className="text-xs font-mono text-slate-400">
                    {finOpsData?.resources?.length ?? 0} resources
                  </span>
                </div>
              </div>

              <ResourceTable resources={finOpsData?.resources ?? []} />

              {finOpsData?.pricingDisclaimer && (
                <div className="px-6 py-3 border-t border-slate-800/40 bg-slate-950/40">
                  <p className="text-xs text-slate-700 font-mono leading-relaxed">
                    ⚠ {finOpsData.pricingDisclaimer}
                  </p>
                </div>
              )}
            </div>

          </div>
        )}

        <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
          <p className="text-xs font-mono text-slate-700">
            mindops-cloud · finops v1.0
          </p>
          <p className="text-xs font-mono text-slate-700">
            powered by azure retail prices api
          </p>
        </div>

      </div>
    </div>
  );
}