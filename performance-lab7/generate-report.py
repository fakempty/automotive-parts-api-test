import json
import os
from pathlib import Path

try:
    import matplotlib.pyplot as plt
    HAS_MPL = True
except ImportError:
    HAS_MPL = False

base_dir = Path(__file__).resolve().parent
results_dir = base_dir / "results"
reports_dir = base_dir / "reports"
reports_dir.mkdir(parents=True, exist_ok=True)

scenarios = ["baseline", "load", "stress"]
rows = []

for sc in scenarios:
    summary_path = results_dir / sc / "k6-summary.json"
    if not summary_path.exists():
        continue
    data = json.loads(summary_path.read_text(encoding="utf-8"))
    metrics = data.get("metrics", {})

    dur = metrics.get("http_req_duration", {}).get("values", {})
    reqs = metrics.get("http_reqs", {}).get("values", {})
    failed = metrics.get("http_req_failed", {}).get("values", {})

    avg_ms = dur.get("avg", 0.0)
    p95_ms = dur.get("p(95)", 0.0)
    rps = reqs.get("rate", 0.0)
    err_rate = (failed.get("rate", 0.0) * 100.0)

    sla_ok = "PASS" if (p95_ms < 500.0 and err_rate < 1.0) else "FAIL"
    if sc == "stress" and sla_ok == "FAIL":
        sla_note = "Expected degradation under stress"
    else:
        sla_note = sla_ok

    rows.append({
        "scenario": sc,
        "avg_ms": round(avg_ms, 2),
        "p95_ms": round(p95_ms, 2),
        "rps": round(rps, 2),
        "err_pct": round(err_rate, 2),
        "sla": sla_note,
    })

md_lines = [
    "# Lab 7 — Performance & Load Testing Summary (k6)",
    "",
    "| Scenario | Avg Response (ms) | p95 Response (ms) | Throughput (RPS) | Error Rate (%) | Threshold / Status |",
    "|---|---:|---:|---:|---:|---|",
]
for r in rows:
    md_lines.append(
        f"| {r['scenario']} | {r['avg_ms']} | {r['p95_ms']} | {r['rps']} | {r['err_pct']}% | {r['sla']} |"
    )

(reports_dir / "summary_metrics.md").write_text("\n".join(md_lines), encoding="utf-8")
print("\n".join(md_lines))

if HAS_MPL and rows:
    labels = [r["scenario"] for r in rows]
    avg_vals = [r["avg_ms"] for r in rows]
    p95_vals = [r["p95_ms"] for r in rows]
    rps_vals = [r["rps"] for r in rows]
    err_vals = [r["err_pct"] for r in rows]

    fig, axes = plt.subplots(1, 3, figsize=(14, 4.5))
    fig.suptitle("Lab 7: Performance Metrics Across Baseline, Load & Stress")

    axes[0].bar(labels, avg_vals, label="Avg (ms)")
    axes[0].bar(labels, p95_vals, alpha=0.65, label="p95 (ms)")
    axes[0].axhline(500, linestyle="--", linewidth=1, label="SLA p95 < 500ms")
    axes[0].set_title("Response Time (ms)")
    axes[0].set_ylabel("ms")
    axes[0].legend()

    axes[1].bar(labels, rps_vals)
    axes[1].set_title("Throughput (req/s)")
    axes[1].set_ylabel("RPS")

    axes[2].bar(labels, err_vals)
    axes[2].axhline(1.0, linestyle="--", linewidth=1, label="SLA < 1%")
    axes[2].set_title("Error Rate (%)")
    axes[2].set_ylabel("%")
    axes[2].legend()

    plt.tight_layout()
    chart_path = reports_dir / "performance_comparison.png"
    plt.savefig(chart_path, dpi=150)
    print(f"\nChart saved to: {chart_path}")
