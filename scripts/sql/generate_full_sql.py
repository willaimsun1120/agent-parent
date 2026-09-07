#!/usr/bin/env python3
"""Merge Flyway migrations into full SQL scripts for each demo."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXAMPLES = ROOT / "agent-examples"
OUT_DIR = Path(__file__).resolve().parent


def merge_demo(demo_name: str, db_name: str, output_file: str, title: str) -> Path:
    migration_dir = EXAMPLES / demo_name / "src/main/resources/db/migration"
    files = sorted(migration_dir.glob("V*.sql"), key=lambda p: int(p.name.split("__")[0][1:]))

    lines = [
        "-- " + "=" * 70,
        f"-- AgentForge - {title}",
        f"-- Database: {db_name}",
        f"-- Generated from Flyway migrations: {demo_name}",
        f"-- Migrations: {', '.join(f.name for f in files)}",
        "-- " + "=" * 70,
        "",
        f"CREATE DATABASE IF NOT EXISTS `{db_name}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;",
        f"USE `{db_name}`;",
        "",
    ]

    for f in files:
        content = f.read_text(encoding="utf-8").strip()
        lines.extend([
            "-- " + "-" * 70,
            f"-- Source: {f.name}",
            "-- " + "-" * 70,
            content,
            "",
            "",
        ])

    out = OUT_DIR / output_file
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"  {out.name} ({len(files)} migrations, {out.stat().st_size:,} bytes)")
    return out


def main() -> None:
    print("Generating full SQL scripts...")
    order = merge_demo(
        "order-cs-example",
        "order_agent_demo",
        "order_agent_demo_full.sql",
        "订单客服 Demo 完整建库脚本",
    )
    hr = merge_demo(
        "hr-cs-example",
        "hr_agent_demo",
        "hr_agent_demo_full.sql",
        "HR 客服 Demo 完整建库脚本",
    )

    combined = OUT_DIR / "all_demos_full.sql"
    combined.write_text(
        "\n".join([
            "-- " + "=" * 70,
            "-- AgentForge - 两个 Demo 完整建库脚本（汇总版）",
            "-- Databases: order_agent_demo + hr_agent_demo",
            "-- Usage: mysql -u root -p < scripts/sql/all_demos_full.sql",
            "-- " + "=" * 70,
            "",
            order.read_text(encoding="utf-8"),
            "",
            "-- " + "#" * 70,
            "-- # HR 客服 Demo",
            "-- " + "#" * 70,
            "",
            hr.read_text(encoding="utf-8"),
        ]),
        encoding="utf-8",
    )
    print(f"  {combined.name} ({combined.stat().st_size:,} bytes)")
    print("Done.")


if __name__ == "__main__":
    main()
