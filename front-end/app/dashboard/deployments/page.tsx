// "use client";

// import { useEffect, useState, useCallback } from "react";
// import {
//   Layers,
//   RefreshCw,
//   Trash2,
//   AlertTriangle,
//   X,
//   ChevronLeft,
//   ChevronRight,
//   Loader2,
//   Terminal,
//   Clock,
//   DollarSign,
//   Hash,
//   Zap,
//   CheckCircle2,
//   XCircle,
//   AlertCircle,
//   MinusCircle,
// } from "lucide-react";
// import api from "@/lib/api";

// // ── Types ─────────────────────────────────────────────────────────────────────

// type DeploymentStatus =
//   | "PENDING"
//   | "APPROVED"
//   | "EXECUTING"
//   | "SUCCESS"
//   | "FAILED"
//   | "DESTROYING"
//   | "DESTROYED"
//   | "DESTROY_FAILED";

// interface DeploymentPlan {
//   planId: string;
//   status: DeploymentStatus;
//   userPrompt: string;
//   costEstimate: number | null;
//   credentialId: string;
//   createdAt: string;
//   updatedAt: string;
//   aiGeneratedConfig?: Record<string, unknown>;
// }

// interface PagedResponse {
//   content: DeploymentPlan[];
//   totalElements: number;
//   totalPages: number;
//   currentPage: number;
//   pageSize: number;
//   isLast: boolean;
// }

// // ── Constants ─────────────────────────────────────────────────────────────────

// const PAGE_SIZE = 50;

// const TEARDOWN_ENABLED_STATUSES: DeploymentStatus[] = ["SUCCESS", "FAILED", "DESTROY_FAILED"];

// // ── Formatters ────────────────────────────────────────────────────────────────

// function fmt(value: number | null | undefined): string {
//   if (value == null) return "—";
//   return new Intl.NumberFormat("en-US", {
//     style: "currency",
//     currency: "USD",
//     minimumFractionDigits: 2,
//     maximumFractionDigits: 2,
//   }).format(value);
// }

// function fmtDate(iso: string | null | undefined): string {
//   if (!iso) return "—";
//   try {
//     return new Date(iso).toLocaleString("en-US", {
//       month: "short",
//       day: "numeric",
//       year: "numeric",
//       hour: "2-digit",
//       minute: "2-digit",
//     });
//   } catch {
//     return iso;
//   }
// }

// function truncate(text: string, max = 72): string {
//   if (!text) return "—";
//   return text.length > max ? text.slice(0, max).trimEnd() + "…" : text;
// }

// // ── Status Badge ──────────────────────────────────────────────────────────────

// interface StatusConfig {
//   label: string;
//   icon: React.ReactNode;
//   classes: string;
//   dotClasses: string;
//   animated: boolean;
// }

// function getStatusConfig(status: DeploymentStatus): StatusConfig {
//   switch (status) {
//     case "PENDING":
//       return {
//         label: "Pending",
//         icon: <Clock className="w-3 h-3" />,
//         classes:
//           "bg-amber-500/10 text-amber-300 border-amber-500/25",
//         dotClasses: "bg-amber-400 animate-pulse",
//         animated: true,
//       };
//     case "APPROVED":
//       return {
//         label: "Approved",
//         icon: <CheckCircle2 className="w-3 h-3" />,
//         classes:
//           "bg-amber-500/10 text-amber-300 border-amber-500/25",
//         dotClasses: "bg-amber-400 animate-pulse",
//         animated: true,
//       };
//     case "EXECUTING":
//       return {
//         label: "Executing",
//         icon: <Loader2 className="w-3 h-3 animate-spin" />,
//         classes:
//           "bg-amber-500/10 text-amber-300 border-amber-500/25",
//         dotClasses: "bg-amber-400 animate-pulse",
//         animated: true,
//       };
//     case "SUCCESS":
//       return {
//         label: "Success",
//         icon: <CheckCircle2 className="w-3 h-3" />,
//         classes:
//           "bg-emerald-500/10 text-emerald-300 border-emerald-500/25",
//         dotClasses: "bg-emerald-400",
//         animated: false,
//       };
//     case "FAILED":
//       return {
//         label: "Failed",
//         icon: <XCircle className="w-3 h-3" />,
//         classes: "bg-rose-500/10 text-rose-300 border-rose-500/25",
//         dotClasses: "bg-rose-400",
//         animated: false,
//       };
//     case "DESTROYING":
//       return {
//         label: "Destroying",
//         icon: <Loader2 className="w-3 h-3 animate-spin" />,
//         classes: "bg-sky-500/10 text-sky-300 border-sky-500/25",
//         dotClasses: "bg-sky-400 animate-pulse",
//         animated: true,
//       };
//     case "DESTROYED":
//       return {
//         label: "Destroyed",
//         icon: <MinusCircle className="w-3 h-3" />,
//         classes:
//           "bg-slate-700/40 text-slate-500 border-slate-600/30",
//         dotClasses: "bg-slate-500",
//         animated: false,
//       };
//     case "DESTROY_FAILED":
//       return {
//         label: "Destroy Failed",
//         icon: <AlertCircle className="w-3 h-3" />,
//         classes: "bg-rose-500/10 text-rose-300 border-rose-500/25",
//         dotClasses: "bg-rose-400",
//         animated: false,
//       };
//     default:
//       return {
//         label: status,
//         icon: null,
//         classes:
//           "bg-slate-700/40 text-slate-400 border-slate-600/30",
//         dotClasses: "bg-slate-500",
//         animated: false,
//       };
//   }
// }

// function StatusBadge({ status }: { status: DeploymentStatus }) {
//   const cfg = getStatusConfig(status);
//   return (
//     <span
//       className={`
//         inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg
//         text-xs font-semibold border
//         ${cfg.classes}
//       `}
//     >
//       <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${cfg.dotClasses}`} />
//       {cfg.label}
//     </span>
//   );
// }

// // ── Confirm Modal ─────────────────────────────────────────────────────────────

// function ConfirmTeardownModal({
//   plan,
//   onConfirm,
//   onCancel,
//   loading,
// }: {
//   plan: DeploymentPlan;
//   onConfirm: () => void;
//   onCancel: () => void;
//   loading: boolean;
// }) {
//   // Close on Escape key
//   useEffect(() => {
//     function handler(e: KeyboardEvent) {
//       if (e.key === "Escape" && !loading) onCancel();
//     }
//     document.addEventListener("keydown", handler);
//     return () => document.removeEventListener("keydown", handler);
//   }, [loading, onCancel]);

//   return (
//     <div
//       className="fixed inset-0 z-50 flex items-center justify-center p-4"
//       role="dialog"
//       aria-modal="true"
//       aria-labelledby="modal-title"
//     >
//       {/* Backdrop */}
//       <div
//         className="absolute inset-0 bg-black/70 backdrop-blur-sm"
//         onClick={() => !loading && onCancel()}
//       />

//       {/* Panel */}
//       <div className="relative w-full max-w-md rounded-2xl border border-slate-700/60 bg-slate-900/95 backdrop-blur-xl shadow-2xl shadow-black/60 p-6 space-y-5">

//         {/* Close button */}
//         {!loading && (
//           <button
//             onClick={onCancel}
//             className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors duration-150"
//           >
//             <X className="w-4 h-4" />
//           </button>
//         )}

//         {/* Warning icon */}
//         <div className="flex items-center gap-4">
//           <div className="w-12 h-12 rounded-2xl bg-rose-500/15 border border-rose-500/30 flex items-center justify-center flex-shrink-0">
//             <AlertTriangle className="w-6 h-6 text-rose-400" />
//           </div>
//           <div>
//             <h2
//               id="modal-title"
//               className="text-base font-bold text-white"
//             >
//               Confirm Teardown
//             </h2>
//             <p className="text-slate-400 text-sm mt-0.5">
//               This action cannot be undone.
//             </p>
//           </div>
//         </div>

//         {/* Body */}
//         <div className="rounded-xl border border-slate-700/50 bg-slate-800/40 p-4 space-y-3">
//           <p className="text-sm text-slate-300 leading-relaxed">
//             This will permanently destroy all cloud resources associated
//             with this deployment plan. All provisioned infrastructure will
//             be deleted from your cloud provider.
//           </p>

//           <div className="pt-2 border-t border-slate-700/50 space-y-2">
//             <div className="flex items-start gap-2">
//               <span className="text-xs font-mono text-slate-500 flex-shrink-0 mt-0.5">
//                 PLAN ID
//               </span>
//               <span className="text-xs font-mono text-sky-300 break-all">
//                 {plan.planId}
//               </span>
//             </div>
//             <div className="flex items-start gap-2">
//               <span className="text-xs font-mono text-slate-500 flex-shrink-0 mt-0.5">
//                 PROMPT
//               </span>
//               <span className="text-xs text-slate-400 leading-relaxed">
//                 {truncate(plan.userPrompt, 80)}
//               </span>
//             </div>
//           </div>
//         </div>

//         {/* Actions */}
//         <div className="flex items-center gap-3 pt-1">
//           <button
//             onClick={onCancel}
//             disabled={loading}
//             className="flex-1 px-4 py-2.5 rounded-xl text-sm font-semibold
//               text-slate-300 bg-slate-800 hover:bg-slate-700
//               border border-slate-700/60 hover:border-slate-600
//               transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
//           >
//             Cancel
//           </button>
//           <button
//             onClick={onConfirm}
//             disabled={loading}
//             className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl
//               text-sm font-semibold text-white
//               bg-rose-600 hover:bg-rose-500 active:scale-[0.98]
//               shadow-lg shadow-rose-500/20 hover:shadow-rose-500/30
//               transition-all duration-150 disabled:opacity-60 disabled:cursor-not-allowed"
//           >
//             {loading ? (
//               <>
//                 <Loader2 className="w-4 h-4 animate-spin" />
//                 Destroying…
//               </>
//             ) : (
//               <>
//                 <Trash2 className="w-4 h-4" />
//                 Yes, Destroy
//               </>
//             )}
//           </button>
//         </div>
//       </div>
//     </div>
//   );
// }

// // ── Skeleton Loader ───────────────────────────────────────────────────────────

// function TableSkeleton() {
//   return (
//     <div className="space-y-px">
//       {[...Array(6)].map((_, i) => (
//         <div
//           key={i}
//           className="h-16 rounded-xl bg-slate-800/40 border border-slate-700/30 animate-pulse"
//           style={{ animationDelay: `${i * 80}ms` }}
//         />
//       ))}
//     </div>
//   );
// }

// // ── Empty State ───────────────────────────────────────────────────────────────

// function EmptyState({ onRefresh }: { onRefresh: () => void }) {
//   return (
//     <div className="flex flex-col items-center justify-center py-24 gap-5 text-center">
//       <div className="w-16 h-16 rounded-2xl bg-slate-800/80 border border-slate-700/60 flex items-center justify-center">
//         <Layers className="w-7 h-7 text-slate-600" />
//       </div>
//       <div className="space-y-1.5">
//         <p className="text-slate-300 font-semibold text-base">
//           No deployments found
//         </p>
//         <p className="text-slate-600 text-sm font-mono max-w-xs">
//           Use the AI Orchestrator to provision your first infrastructure
//           deployment.
//         </p>
//       </div>
//       <button
//         onClick={onRefresh}
//         className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm
//           text-slate-400 hover:text-slate-200 bg-slate-800/60 hover:bg-slate-800
//           border border-slate-700/60 hover:border-slate-600
//           transition-all duration-150"
//       >
//         <RefreshCw className="w-3.5 h-3.5" />
//         Refresh
//       </button>
//     </div>
//   );
// }

// // ── Pagination ────────────────────────────────────────────────────────────────

// function Pagination({
//   page,
//   totalPages,
//   totalElements,
//   pageSize,
//   onPrev,
//   onNext,
//   loading,
// }: {
//   page: number;
//   totalPages: number;
//   totalElements: number;
//   pageSize: number;
//   onPrev: () => void;
//   onNext: () => void;
//   loading: boolean;
// }) {
//   const from = page * pageSize + 1;
//   const to = Math.min((page + 1) * pageSize, totalElements);

//   return (
//     <div className="flex items-center justify-between px-6 py-3 border-t border-slate-800/60">
//       <p className="text-xs font-mono text-slate-500">
//         {totalElements === 0
//           ? "0 results"
//           : `${from}–${to} of ${totalElements}`}
//       </p>
//       <div className="flex items-center gap-2">
//         <button
//           onClick={onPrev}
//           disabled={page === 0 || loading}
//           className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
//             text-slate-400 hover:text-slate-200 bg-slate-800/60 hover:bg-slate-800
//             border border-slate-700/60 hover:border-slate-600
//             transition-all duration-150 disabled:opacity-40 disabled:cursor-not-allowed"
//         >
//           <ChevronLeft className="w-3.5 h-3.5" />
//           Prev
//         </button>
//         <span className="text-xs font-mono text-slate-500 px-1">
//           {page + 1} / {Math.max(totalPages, 1)}
//         </span>
//         <button
//           onClick={onNext}
//           disabled={page + 1 >= totalPages || loading}
//           className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs
//             text-slate-400 hover:text-slate-200 bg-slate-800/60 hover:bg-slate-800
//             border border-slate-700/60 hover:border-slate-600
//             transition-all duration-150 disabled:opacity-40 disabled:cursor-not-allowed"
//         >
//           Next
//           <ChevronRight className="w-3.5 h-3.5" />
//         </button>
//       </div>
//     </div>
//   );
// }

// // ── Main Page ─────────────────────────────────────────────────────────────────

// export default function DeploymentsPage() {
//   const [data, setData] = useState<PagedResponse | null>(null);
//   const [loading, setLoading] = useState(true);
//   const [error, setError] = useState<string>("");
//   const [page, setPage] = useState(0);

//   // Teardown modal state
//   const [pendingTeardown, setPendingTeardown] = useState<DeploymentPlan | null>(null);
//   const [teardownLoading, setTeardownLoading] = useState(false);
//   const [teardownError, setTeardownError] = useState<string>("");

//   // ── Fetch deployments ────────────────────────────────────────────────────

//   const fetchDeployments = useCallback(async (pageNum: number) => {
//     setLoading(true);
//     setError("");
//     try {
//       const res = await api.get<PagedResponse>(
//         `/deployments?page=${pageNum}&size=${PAGE_SIZE}`
//       );
//       setData(res.data);
//     } catch (err: unknown) {
//       const msg =
//         (err as { response?: { data?: { message?: string } } })?.response?.data
//           ?.message ??
//         "Failed to load deployments. Please try again.";
//       setError(msg);
//     } finally {
//       setLoading(false);
//     }
//   }, []);

//   useEffect(() => {
//     fetchDeployments(page);
//   }, [page, fetchDeployments]);

//   // ── Teardown ─────────────────────────────────────────────────────────────

//   async function handleConfirmTeardown() {
//     if (!pendingTeardown) return;
//     setTeardownLoading(true);
//     setTeardownError("");
//     try {
//       await api.delete(`/api/v1/deployments/${pendingTeardown.planId}`);
//       setPendingTeardown(null);
//       // Refresh current page so status updates to DESTROYING
//       await fetchDeployments(page);
//     } catch (err: unknown) {
//       const msg =
//         (err as { response?: { data?: { message?: string } } })?.response?.data
//           ?.message ?? "Teardown request failed. Please try again.";
//       setTeardownError(msg);
//     } finally {
//       setTeardownLoading(false);
//     }
//   }

//   function handleCancelTeardown() {
//     if (teardownLoading) return;
//     setPendingTeardown(null);
//     setTeardownError("");
//   }

//   // ── Derived ──────────────────────────────────────────────────────────────

//   const deployments = data?.content ?? [];
//   const totalElements = data?.totalElements ?? 0;
//   const totalPages = data?.totalPages ?? 0;

//   const stats = {
//     success: deployments.filter((d) => d.status === "SUCCESS").length,
//     active: deployments.filter((d) =>
//       ["PENDING", "APPROVED", "EXECUTING"].includes(d.status)
//     ).length,
//     failed: deployments.filter((d) =>
//       ["FAILED", "DESTROY_FAILED"].includes(d.status)
//     ).length,
//     totalCost: deployments.reduce(
//       (acc, d) => acc + (d.costEstimate ?? 0),
//       0
//     ),
//   };

//   // ── Render ────────────────────────────────────────────────────────────────

//   return (
//     <>
//       <div className="min-h-screen bg-slate-950 text-slate-100">
//         {/* Background grid */}
//         <div
//           className="fixed inset-0 opacity-[0.015] pointer-events-none"
//           style={{
//             backgroundImage:
//               "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)",
//             backgroundSize: "48px 48px",
//           }}
//         />
//         <div className="fixed top-0 left-1/3 w-[600px] h-[350px] bg-violet-500/4 rounded-full blur-3xl pointer-events-none" />

//         <div className="relative max-w-7xl mx-auto px-6 py-10 space-y-8">

//           {/* ── Header ──────────────────────────────────────────────────── */}
//           <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
//             <div className="flex items-center gap-3">
//               <div className="w-10 h-10 rounded-xl bg-violet-500/15 border border-violet-500/30 flex items-center justify-center">
//                 <Layers className="w-5 h-5 text-violet-400" />
//               </div>
//               <div>
//                 <h1 className="text-2xl font-bold tracking-tight text-white">
//                   Deployment Tracker
//                 </h1>
//                 <p className="text-slate-500 text-sm">
//                   Lifecycle management and infrastructure state.
//                 </p>
//               </div>
//             </div>

//             <button
//               onClick={() => fetchDeployments(page)}
//               disabled={loading}
//               className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-mono
//                 text-slate-400 hover:text-slate-200 bg-slate-900/60 hover:bg-slate-800
//                 border border-slate-700/60 hover:border-slate-600
//                 transition-all duration-150 disabled:opacity-50 self-start sm:self-auto"
//             >
//               <RefreshCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
//               Refresh
//             </button>
//           </div>

//           {/* ── Stats strip ─────────────────────────────────────────────── */}
//           {!loading && deployments.length > 0 && (
//             <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
//               {[
//                 {
//                   label: "Total",
//                   value: String(totalElements),
//                   icon: Hash,
//                   color: "text-slate-300",
//                   bg: "bg-slate-800/60 border-slate-700/40",
//                 },
//                 {
//                   label: "Success",
//                   value: String(stats.success),
//                   icon: CheckCircle2,
//                   color: "text-emerald-300",
//                   bg: "bg-emerald-500/8 border-emerald-500/20",
//                 },
//                 {
//                   label: "Active",
//                   value: String(stats.active),
//                   icon: Zap,
//                   color: "text-amber-300",
//                   bg: "bg-amber-500/8 border-amber-500/20",
//                 },
//                 {
//                   label: "Est. Cost",
//                   value: fmt(stats.totalCost) + "/mo",
//                   icon: DollarSign,
//                   color: "text-sky-300",
//                   bg: "bg-sky-500/8 border-sky-500/20",
//                 },
//               ].map((s) => (
//                 <div
//                   key={s.label}
//                   className={`flex items-center gap-3 px-4 py-3 rounded-xl border ${s.bg}`}
//                 >
//                   <s.icon className={`w-4 h-4 ${s.color} flex-shrink-0`} />
//                   <div>
//                     <p className="text-xs font-mono text-slate-500 uppercase tracking-wider">
//                       {s.label}
//                     </p>
//                     <p className={`text-sm font-bold ${s.color}`}>{s.value}</p>
//                   </div>
//                 </div>
//               ))}
//             </div>
//           )}

//           {/* ── Error banner ─────────────────────────────────────────────── */}
//           {error && !loading && (
//             <div className="flex items-center gap-3 p-4 rounded-xl border border-rose-500/25 bg-rose-500/8">
//               <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
//               <p className="text-sm text-rose-300 font-mono">{error}</p>
//               <button
//                 onClick={() => fetchDeployments(page)}
//                 className="ml-auto text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1.5 flex-shrink-0"
//               >
//                 <RefreshCw className="w-3 h-3" /> Retry
//               </button>
//             </div>
//           )}

//           {/* ── Teardown error ────────────────────────────────────────────── */}
//           {teardownError && (
//             <div className="flex items-center gap-3 p-4 rounded-xl border border-rose-500/25 bg-rose-500/8">
//               <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
//               <p className="text-sm text-rose-300 font-mono">{teardownError}</p>
//               <button
//                 onClick={() => setTeardownError("")}
//                 className="ml-auto text-slate-500 hover:text-slate-300 flex-shrink-0"
//               >
//                 <X className="w-4 h-4" />
//               </button>
//             </div>
//           )}

//           {/* ── Table card ───────────────────────────────────────────────── */}
//           <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden">

//             {/* Table header */}
//             <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/60">
//               <p className="text-sm font-semibold text-slate-200">
//                 All Deployments
//               </p>
//               <span className="px-2.5 py-1 rounded-lg bg-slate-800/80 border border-slate-700/40 text-xs font-mono text-slate-400">
//                 {loading ? "Loading…" : `${totalElements} total`}
//               </span>
//             </div>

//             {/* Loading */}
//             {loading && (
//               <div className="p-6">
//                 <TableSkeleton />
//               </div>
//             )}

//             {/* Empty */}
//             {!loading && !error && deployments.length === 0 && (
//               <EmptyState onRefresh={() => fetchDeployments(page)} />
//             )}

//             {/* Table */}
//             {!loading && deployments.length > 0 && (
//               <div className="overflow-x-auto">
//                 <table className="w-full text-sm">
//                   <thead>
//                     <tr className="border-b border-slate-800/80">
//                       {[
//                         { label: "Plan ID", icon: Hash },
//                         { label: "Prompt", icon: Terminal },
//                         { label: "Cost", icon: DollarSign },
//                         { label: "Created", icon: Clock },
//                         { label: "Status", icon: null },
//                         { label: "Actions", icon: null },
//                       ].map((col) => (
//                         <th
//                           key={col.label}
//                           className="text-left px-5 py-3 text-xs font-mono text-slate-500 uppercase tracking-widest font-normal whitespace-nowrap"
//                         >
//                           <div className="flex items-center gap-1.5">
//                             {col.icon && (
//                               <col.icon className="w-3 h-3 text-slate-600" />
//                             )}
//                             {col.label}
//                           </div>
//                         </th>
//                       ))}
//                     </tr>
//                   </thead>
//                   <tbody>
//                     {deployments.map((plan, idx) => {
//                       const canTeardown = TEARDOWN_ENABLED_STATUSES.includes(
//                         plan.status
//                       );
//                       const isDestroyed = plan.status === "DESTROYED";

//                       return (
//                         <tr
//                           key={plan.planId}
//                           className={`
//                             border-b border-slate-800/40 transition-colors duration-150
//                             hover:bg-slate-800/25
//                             ${isDestroyed ? "opacity-50" : ""}
//                             ${idx === deployments.length - 1 ? "border-b-0" : ""}
//                           `}
//                         >
//                           {/* Plan ID */}
//                           <td className="px-5 py-4 whitespace-nowrap">
//                             <div className="flex items-center gap-2">
//                               <div className="w-5 h-5 rounded-md bg-slate-800 border border-slate-700/60 flex items-center justify-center flex-shrink-0">
//                                 <Hash className="w-2.5 h-2.5 text-slate-500" />
//                               </div>
//                               <span className="font-mono text-xs text-sky-300 hover:text-sky-200 transition-colors">
//                                 {plan.planId}
//                               </span>
//                             </div>
//                           </td>

//                           {/* Prompt */}
//                           <td className="px-5 py-4 max-w-xs">
//                             <p
//                               className="text-slate-300 text-xs leading-relaxed"
//                               title={plan.userPrompt}
//                             >
//                               {truncate(plan.userPrompt, 68)}
//                             </p>
//                           </td>

//                           {/* Cost */}
//                           <td className="px-5 py-4 whitespace-nowrap">
//                             <span
//                               className={`font-mono text-xs font-semibold ${
//                                 plan.costEstimate != null &&
//                                 plan.costEstimate > 0
//                                   ? "text-emerald-300"
//                                   : "text-slate-600"
//                               }`}
//                             >
//                               {plan.costEstimate != null
//                                 ? `${fmt(plan.costEstimate)}/mo`
//                                 : "—"}
//                             </span>
//                           </td>

//                           {/* Created */}
//                           <td className="px-5 py-4 whitespace-nowrap">
//                             <span className="font-mono text-xs text-slate-400">
//                               {fmtDate(plan.createdAt)}
//                             </span>
//                           </td>

//                           {/* Status */}
//                           <td className="px-5 py-4 whitespace-nowrap">
//                             <StatusBadge status={plan.status} />
//                           </td>

//                           {/* Actions */}
//                           <td className="px-5 py-4 whitespace-nowrap">
//                             <button
//                               onClick={() => {
//                                 if (canTeardown) {
//                                   setTeardownError("");
//                                   setPendingTeardown(plan);
//                                 }
//                               }}
//                               disabled={!canTeardown}
//                               title={
//                                 canTeardown
//                                   ? "Destroy cloud resources"
//                                   : `Cannot destroy — status is ${plan.status}`
//                               }
//                               className={`
//                                 flex items-center gap-1.5 px-3 py-1.5 rounded-lg
//                                 text-xs font-semibold border
//                                 transition-all duration-150
//                                 focus:outline-none focus:ring-2 focus:ring-rose-500/30
//                                 ${
//                                   canTeardown
//                                     ? `bg-rose-500/10 text-rose-400 border-rose-500/25
//                                        hover:bg-rose-500/20 hover:text-rose-300 hover:border-rose-500/40
//                                        active:scale-95`
//                                     : `bg-slate-800/40 text-slate-600 border-slate-700/30
//                                        cursor-not-allowed`
//                                 }
//                               `}
//                             >
//                               <Trash2 className="w-3 h-3" />
//                               Destroy
//                             </button>
//                           </td>
//                         </tr>
//                       );
//                     })}
//                   </tbody>
//                 </table>
//               </div>
//             )}

//             {/* Pagination */}
//             {!loading && totalElements > 0 && (
//               <Pagination
//                 page={page}
//                 totalPages={totalPages}
//                 totalElements={totalElements}
//                 pageSize={PAGE_SIZE}
//                 onPrev={() => setPage((p) => Math.max(0, p - 1))}
//                 onNext={() => setPage((p) => p + 1)}
//                 loading={loading}
//               />
//             )}
//           </div>

//           {/* ── Footer ──────────────────────────────────────────────────── */}
//           <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
//             <p className="text-xs font-mono text-slate-700">
//               mindops-cloud · deployment tracker v1.0
//             </p>
//             <p className="text-xs font-mono text-slate-700">
//               auto-refresh not enabled · use refresh button
//             </p>
//           </div>

//         </div>
//       </div>

//       {/* ── Teardown modal (rendered at root, outside scroll container) ─── */}
//       {pendingTeardown && (
//         <ConfirmTeardownModal
//           plan={pendingTeardown}
//           onConfirm={handleConfirmTeardown}
//           onCancel={handleCancelTeardown}
//           loading={teardownLoading}
//         />
//       )}
//     </>
//   );
// }

"use client";

import { useEffect, useState, useCallback } from "react";
import {
  Layers,
  RefreshCw,
  AlertCircle,
  PowerOff,
  Loader2,
  CheckCircle2,
  XCircle,
  Clock,
  Trash2,
  ServerCrash
} from "lucide-react";
import api from "@/lib/api";

// ── Types ─────────────────────────────────────────────────────────────────────

interface DeploymentPlanResponse {
  planId: string;
  status: string;
  userPrompt: string;
  costEstimate: number | null;
  createdAt: string;
}

interface PagedResponse<T> {
  content: T[];
  totalElements: number;
}

// ── Formatting ────────────────────────────────────────────────────────────────

function fmtDate(iso: string) {
  try {
    return new Date(iso).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return "—";
  }
}

function fmtCost(val: number | null) {
  if (val == null) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(val) + "/mo";
}

// ── Status Badge Component ────────────────────────────────────────────────────

function StatusBadge({ status }: { status: string }) {
  let config = { bg: "bg-slate-800", text: "text-slate-400", border: "border-slate-700", icon: Clock, pulse: false };

  switch (status) {
    case "PENDING":
    case "IN_PROGRESS":
      config = { bg: "bg-amber-500/10", text: "text-amber-400", border: "border-amber-500/20", icon: Loader2, pulse: true };
      break;
    case "SUCCESS":
      config = { bg: "bg-emerald-500/10", text: "text-emerald-400", border: "border-emerald-500/20", icon: CheckCircle2, pulse: false };
      break;
    case "FAILED":
    case "DESTROY_FAILED":
      config = { bg: "bg-rose-500/10", text: "text-rose-400", border: "border-rose-500/20", icon: XCircle, pulse: false };
      break;
    case "DESTROYING":
      config = { bg: "bg-sky-500/10", text: "text-sky-400", border: "border-sky-500/20", icon: Loader2, pulse: true };
      break;
    case "DESTROYED":
      config = { bg: "bg-slate-800/50", text: "text-slate-500", border: "border-slate-700/50", icon: Trash2, pulse: false };
      break;
  }

  const Icon = config.icon;

  return (
    <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border ${config.bg} ${config.border}`}>
      <Icon className={`w-3 h-3 ${config.text} ${config.pulse ? "animate-spin" : ""}`} />
      <span className={`text-[10px] font-mono font-bold uppercase tracking-wider ${config.text}`}>
        {status}
      </span>
    </div>
  );
}

// ── Main Page Component ───────────────────────────────────────────────────────

export default function DeploymentsTrackerPage() {
  const [deployments, setDeployments] = useState<DeploymentPlanResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Teardown Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [planToDestroy, setPlanToDestroy] = useState<string | null>(null);
  const [isDestroying, setIsDestroying] = useState(false);

  // ── API Calls ───────────────────────────────────────────────────────────────

  const fetchDeployments = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      // FIXED: Removed /api/v1 prefix. Calling GET /deployments
      const res = await api.get<PagedResponse<DeploymentPlanResponse>>("/deployments?page=0&size=50");
      setDeployments(res.data.content || []);
      setTotal(res.data.totalElements || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || "An unexpected error occurred. Please try again later.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDeployments();
  }, [fetchDeployments]);

  async function confirmTeardown() {
    if (!planToDestroy) return;
    setIsDestroying(true);
    try {
      // FIXED: Removed /api/v1 prefix. Calling DELETE /deployments/{planId}
      await api.delete(`/deployments/${planToDestroy}`);
      setModalOpen(false);
      setPlanToDestroy(null);
      fetchDeployments(); // Refresh table to show "DESTROYING" state
    } catch (err: any) {
      alert(err.response?.data?.message || "Failed to initiate teardown.");
    } finally {
      setIsDestroying(false);
    }
  }

  function openTeardownModal(planId: string) {
    setPlanToDestroy(planId);
    setModalOpen(true);
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pb-20">
      
      {/* Ambient Background */}
      <div className="fixed inset-0 opacity-[0.015] pointer-events-none" style={{ backgroundImage: "linear-gradient(rgba(148,163,184,1) 1px, transparent 1px), linear-gradient(90deg, rgba(148,163,184,1) 1px, transparent 1px)", backgroundSize: "48px 48px" }} />
      <div className="fixed top-0 left-1/4 w-[600px] h-[400px] bg-sky-500/5 rounded-full blur-3xl pointer-events-none" />

      <div className="relative max-w-6xl mx-auto px-6 py-10 space-y-8">
        
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-violet-500/15 border border-violet-500/30 flex items-center justify-center">
              <Layers className="w-6 h-6 text-violet-400" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white">Deployment Tracker</h1>
              <p className="text-slate-500 text-sm">Lifecycle management and infrastructure state.</p>
            </div>
          </div>
          
          <button
            onClick={fetchDeployments}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-mono text-slate-300 bg-slate-900/60 border border-slate-700/60 hover:bg-slate-800 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refresh Data
          </button>
        </div>

        {/* Error State */}
        {error && (
          <div className="flex items-center gap-3 p-4 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-300 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <p className="font-mono">{error}</p>
          </div>
        )}

        {/* The Data Table Container */}
        <div className="rounded-2xl border border-slate-700/50 bg-slate-900/40 backdrop-blur-sm overflow-hidden shadow-2xl">
          
          {/* Table Header Row */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800/80 bg-slate-900/60">
            <h2 className="text-sm font-semibold text-slate-200">Execution History</h2>
            <div className="px-2.5 py-1 rounded bg-slate-800 text-xs font-mono text-slate-400 border border-slate-700">
              {total} Total Records
            </div>
          </div>

          {/* Table */}
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-slate-900/40 text-slate-500 font-mono text-xs uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4 font-medium">Plan ID</th>
                  <th className="px-6 py-4 font-medium">Architecture Prompt</th>
                  <th className="px-6 py-4 font-medium">Est. Cost</th>
                  <th className="px-6 py-4 font-medium">Created (UTC)</th>
                  <th className="px-6 py-4 font-medium">Status</th>
                  <th className="px-6 py-4 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {loading && deployments.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-slate-500">
                      <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 opacity-50" />
                      <p className="font-mono text-xs">Fetching infrastructure state...</p>
                    </td>
                  </tr>
                ) : deployments.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-16 text-center text-slate-500">
                      <ServerCrash className="w-8 h-8 mx-auto mb-3 opacity-20" />
                      <p className="font-mono text-sm">No deployments found.</p>
                      <p className="text-xs mt-1">Go to the AI Orchestrator to provision infrastructure.</p>
                    </td>
                  </tr>
                ) : (
                  deployments.map((dep) => {
                    // Safety check: Only SUCCESS or FAILED plans can be destroyed
                    const canDestroy = dep.status === "SUCCESS" || dep.status === "FAILED";
                    
                    return (
                      <tr key={dep.planId} className="hover:bg-slate-800/30 transition-colors">
                        <td className="px-6 py-4 font-mono text-xs text-slate-300">{dep.planId}</td>
                        <td className="px-6 py-4">
                          <p className="text-slate-300 truncate max-w-[250px] text-xs leading-relaxed" title={dep.userPrompt}>
                            {dep.userPrompt || "—"}
                          </p>
                        </td>
                        <td className="px-6 py-4 font-mono text-emerald-400/90 text-xs">{fmtCost(dep.costEstimate)}</td>
                        <td className="px-6 py-4 font-mono text-slate-500 text-xs">{fmtDate(dep.createdAt)}</td>
                        <td className="px-6 py-4"><StatusBadge status={dep.status} /></td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => openTeardownModal(dep.planId)}
                            disabled={!canDestroy}
                            title={!canDestroy ? "Teardown unavailable for this state" : "Destroy Infrastructure"}
                            className={`p-2 rounded-lg transition-all ${
                              canDestroy 
                                ? "text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 border border-transparent hover:border-rose-500/20" 
                                : "text-slate-700 cursor-not-allowed"
                            }`}
                          >
                            <PowerOff className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Teardown Confirmation Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="bg-slate-900 border border-rose-500/30 rounded-2xl p-6 max-w-md w-full shadow-2xl shadow-rose-900/20 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-4 mb-4">
              <div className="w-12 h-12 rounded-full bg-rose-500/10 flex items-center justify-center flex-shrink-0">
                <AlertCircle className="w-6 h-6 text-rose-500" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Confirm Teardown</h3>
                <p className="text-slate-400 text-xs font-mono mt-0.5">Plan: {planToDestroy}</p>
              </div>
            </div>
            <p className="text-sm text-slate-300 mb-6 leading-relaxed">
              Are you absolutely sure? This action will trigger an asynchronous destruction pipeline. <span className="text-rose-400 font-semibold">All cloud resources associated with this plan will be permanently deleted.</span> This cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setModalOpen(false)}
                disabled={isDestroying}
                className="px-4 py-2 rounded-lg text-sm font-medium text-slate-300 hover:bg-slate-800 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={confirmTeardown}
                disabled={isDestroying}
                className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-rose-600 hover:bg-rose-500 text-white transition-colors shadow-lg shadow-rose-900/20 disabled:opacity-50"
              >
                {isDestroying ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                {isDestroying ? "Initiating..." : "Destroy Infrastructure"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}