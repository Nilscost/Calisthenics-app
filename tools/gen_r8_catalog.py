#!/usr/bin/env python3
"""Import the R8 exercise contract into the starter catalog and app assets.

The starter catalog remains authored by gen_starter_catalog.py. This script
regenerates that base, imports the hand-reviewed R8 contract, and writes the
same canonical JSON into content/starter and app assets. It does not generate
clips or thumbnails; those are installed by the pose/render track.

Run: python3 tools/gen_r8_catalog.py
"""
from __future__ import annotations

import json
import pathlib
import subprocess
import sys
from collections import defaultdict
from typing import Any

from progression_routes import (
    CONDITIONING_POOL_IDS, EXPLICIT_LIBRARY_IDS, EXTRA_MEMBERSHIPS, EXTRAS, PREHAB_IDS,
    ROUTE_GATE_TIERS,
    PROGRESSION_GROUP_OVERRIDES, ROUTES,
)

ROOT = pathlib.Path(__file__).resolve().parent.parent
CONTRACT = ROOT / "docs/23-new-exercises.json"
BASE_GENERATOR = ROOT / "tools/gen_starter_catalog.py"
CONTENT_CATALOG = ROOT / "content/starter/catalog.json"
ASSET_CATALOG = ROOT / "app/src/main/assets/catalog.json"
CONTENT_META = ROOT / "content/starter/catalog_meta.json"
ASSET_META = ROOT / "app/src/main/assets/catalog_meta.json"
CLIP_META = ROOT / "app/src/main/assets/clip_meta.json"
REVIEW_REPORT = ROOT / "work/r8-import-review.json"
ROUTE_REPORT = ROOT / "docs/29-progression-routes.json"

DEMONSTRATION_HOLD = {
    "chin-tuck-seated", "stretch-neck-side", "stretch-twist-straight-leg",
    "stretch-twist-easy", "stretch-twist-half", "stretch-twist-three-quarter",
    "stretch-twist-full", "pilates-roll-up", "pilates-roll-over", "pilates-seal",
    "pilates-saw", "pilates-spine-twist", "pilates-double-leg-kick",
    # Clips do not yet demonstrate these taught positions correctly. They stay
    # in their authored routes, but are withheld from training until reviewed.
    "stretch-thoracic", "scapular-pushup-wall", "scapular-pushup-knee",
    "scapular-pushup", "support-scapular-shrug", "wrist-palm-pulses",
    "chin-nod-supine", "chin-tuck-head-lift",
}

# The six Explosive Calisthenics push-up variations are intentionally capped at
# three reps per set. Other drills do not inherit this cap.
EC_PUSHUP_CAPS = {
    "pushup-pop-up-incline",
    "pushup-kneeling-push-off",
    "pushup-pop-up",
    "pushup-pop-up-wide-close",
    "pushup-clap",
    "pushup-chest-strike",
}

# This is the one comparable group confirmed by the owner. Other sibling edges
# stay ordinary progression branches unless explicitly reviewed into a group.
CONFIRMED_EXERCISE_GROUPS = [
    {
        "id": "pushup-feet-elevated-comparable",
        "mainVariationId": "pushup-feet-elevated",
        "alternativeVariationIds": ["pushup-diamond"],
    }
]
REVIEWED_ROTATION_GROUPS = CONFIRMED_EXERCISE_GROUPS + [
    {"id": "calf-bent-knee-level", "mainVariationId": "calf-raise", "alternativeVariationIds": ["calf-raise-bent-knee"]},
    {"id": "calf-single-bent-knee-level", "mainVariationId": "calf-raise-single", "alternativeVariationIds": ["calf-raise-single-bent-knee"]},
    {"id": "calf-step-bent-knee-level", "mainVariationId": "calf-raise-step", "alternativeVariationIds": ["calf-raise-step-bent-knee"]},
    {"id": "calf-step-single-bent-knee-level", "mainVariationId": "calf-raise-step-single", "alternativeVariationIds": ["calf-raise-step-single-bent-knee"]},
    {"id": "v-up-tuck-level", "mainVariationId": "v-up", "alternativeVariationIds": ["v-up-tuck", "v-up-straddle"]},
    {"id": "floor-legraise-entry-level", "mainVariationId": "leg-raise-lying", "alternativeVariationIds": ["seated-knee-raise", "reverse-crunch", "crunch", "sit-up", "rower-situp", "curl-up-mcgill"]},
    {"id": "crunch-flexion-level", "mainVariationId": "crunch", "alternativeVariationIds": ["sit-up", "rower-situp", "curl-up-mcgill"]},
    {"id": "core-rotation-level", "mainVariationId": "bicycle-crunch", "alternativeVariationIds": ["russian-twist"]},
    {"id": "side-plank-entry-level", "mainVariationId": "side-plank", "alternativeVariationIds": ["side-plank-knee"]},
    {"id": "side-plank-core-variety", "mainVariationId": "side-plank", "alternativeVariationIds": ["bicycle-crunch", "russian-twist", "pallof-hold", "pallof-press", "bird-dog"]},
    {"id": "anti-rotation-plank-hold", "mainVariationId": "plank-long-lever", "alternativeVariationIds": ["plank-contralateral"]},
    {"id": "anti-rotation-plank-reps", "mainVariationId": "plank-shoulder-tap", "alternativeVariationIds": ["plank-arm-leg-reach"]},
]

REPS_BY_CLASS = {
    "strength": [5, 6, 7, 8, 10],
    "core": [8, 9, 10, 11, 12],
    "prehab": [8, 10, 12, 15, 20],
    "conditioning": [5, 8, 10, 12, 15],
    "pilates": [3, 5, 6, 8, 10],
    "negative": [1, 2, 3, 4, 5],
    "power": [1, 2, 3, 3, 3],
    "power_quality": [2, 3, 4, 5, 6],
    "power_hops": [3, 4, 5, 6, 8],
    "power_landing": [2, 3, 4, 5, 6],
}
HOLD_BY_CLASS = {
    "hold_short": [3, 5, 8, 10, 15],
    "hold_10": [5, 8, 10, 15, 20],
    "hold_15": [8, 10, 15, 20, 30],
    "hold_20": [10, 15, 20, 30, 45],
    "hold_long": [15, 20, 30, 45, 60],
}

POWER_FAMILIES = {"plyo", "plyo-lateral", "plyo-box"}
CONDITIONING_FAMILIES = {"conditioning", "agility", "crawl", "army-drill"}
PREHAB_FAMILIES = {"prehab-shoulder", "prehab-wrist", "prehab-hip", "prehab-knee", "neck"}
CORE_FAMILIES = {
    "rollout", "bird-dog", "hanging-legraise", "dragon-flag", "crunch",
    "twist-hold", "flag", "pilates", "ring-support", "ring-dip", "ring-pushup", "ring-mu",
}
LONG_HOLD_FAMILIES = {"handstand", "lever", "german-hang", "flag", "ring-support", "ring-mu"}

# The target schedule is intentionally specific to the movement quality. Max
# targets here are ordinary progression targets, not per-set ceilings.
POWER_TARGETS = {
    "landing-stick": "power_landing", "box-step-off-landing": "power_landing",
    "pogo-hop": "power_hops", "ankle-hop-side-to-side": "power_hops",
    "ankle-hop-hip-twist": "power_hops", "cone-hops-front": "power_hops",
    "cone-hops-lateral": "power_hops", "skater-bound": "power_hops",
    "hop-single-leg-stick": "power_landing", "hop-single-leg-lateral": "power_hops",
    "zigzag-hop": "power_hops", "bounding-alternate": "power_hops",
    "bounding-single-leg": "power_hops", "box-push-off-alternating": "power_quality",
}
POWER_LOW_REP_IDS = {
    "jump-squat", "jump-vertical", "jump-butt-kick", "jump-tuck-slap", "jump-tuck",
    "jump-tuck-catch", "jump-split", "jump-split-cycle", "jump-block", "jump-lateral",
    "jump-broad", "jump-broad-consecutive", "box-push-off-single", "box-jump",
    "box-jump-rebound", "depth-jump",
}


def _round_up_5(n: int) -> int:
    return ((n + 4) // 5) * 5


def _policy_class(exercise: dict[str, Any]) -> str:
    family = exercise["familyId"]
    if exercise["kind"] == "HOLD":
        default = exercise.get("defaultSeconds", 10)
        if default <= 5:
            return "hold_short"
        if default <= 10:
            return "hold_10"
        if default <= 15:
            return "hold_15"
        if default <= 20:
            return "hold_20"
        return "hold_long"
    if family in POWER_FAMILIES:
        return POWER_TARGETS.get(exercise["id"], "power" if exercise["id"] in POWER_LOW_REP_IDS else "power_quality")
    if exercise["id"] in EC_PUSHUP_CAPS:
        return "power"
    if family in CONDITIONING_FAMILIES:
        return "conditioning"
    if family in PREHAB_FAMILIES:
        return "prehab"
    if family == "pilates":
        return "pilates"
    if "negative" in exercise["id"] or "negative" in exercise["name"].lower():
        return "negative"
    if family in CORE_FAMILIES or "CORE" in exercise.get("areas", []):
        return "core"
    return "strength"


def _recovery_seconds(exercise: dict[str, Any], policy_class: str) -> int:
    if policy_class in {"power", "conditioning"} and exercise["familyId"] in POWER_FAMILIES:
        return 120
    if policy_class == "power":
        return 120
    if policy_class == "conditioning":
        return 75
    if exercise["familyId"].startswith("ring-") or exercise["familyId"] in LONG_HOLD_FAMILIES:
        return 90
    if policy_class == "prehab":
        return 45
    if policy_class == "pilates":
        return 45
    return 60


def _tier_targets(exercise: dict[str, Any], policy_class: str) -> list[dict[str, Any]]:
    if exercise["kind"] == "HOLD":
        values = HOLD_BY_CLASS[policy_class]
        return [{"type": "HOLD_SECONDS", "value": n} for n in values]
    values = REPS_BY_CLASS[policy_class]
    return [{"type": "REPS", "value": n} for n in values]


def _policy(exercise: dict[str, Any], all_ids: set[str]) -> dict[str, Any]:
    policy_class = _policy_class(exercise)
    recovery = _recovery_seconds(exercise, policy_class)
    targets = _tier_targets(exercise, policy_class)
    tiers = []
    for index, target in enumerate(targets, 1):
        value = target["value"]
        window = value + 3 if target["type"] == "HOLD_SECONDS" else max(30, _round_up_5(value * 6 + 15))
        tiers.append({
            "index": index,
            "target": target,
            "workWindowSeconds": window,
            "minRecoverySeconds": recovery,
        })

    prereq_ids: list[str] = []
    # insertAfter is source ordering metadata, not an app gate. Only explicit
    # app-only alsoRequires gates survive into prerequisiteRule.
    prereq_ids.extend(exercise.get("alsoRequires", []))
    prereq_ids = list(dict.fromkeys(prereq_ids))
    unknown = sorted(set(prereq_ids) - all_ids)
    if unknown:
        raise ValueError(f"{exercise['id']}: prerequisite ids are absent from the app catalog: {unknown}")

    result: dict[str, Any] = {
        "id": "pol-" + exercise["id"],
        "variationId": exercise["id"],
        "version": 1,
        "tiers": tiers,
        "evidenceSourceIds": exercise.get("sourceIds", []),
        "policyKind": "PRODUCT_HEURISTIC",
        "approved": False,
    }
    if prereq_ids:
        result["prerequisiteRule"] = {"allOf": [
            [{"variationTierMet": {"variationId": prerequisite, "tier": 5}}]
            for prerequisite in prereq_ids
        ]}
    if exercise["id"] in EC_PUSHUP_CAPS:
        result["maxRepsPerSet"] = 3
    return result


def _requirement_sets(exercise: dict[str, Any]) -> list[dict[str, Any]]:
    sets = []
    for alternative in exercise.get("equipment", [{}]):
        result: dict[str, Any] = {}
        if alternative.get("capabilities"):
            result["capabilities"] = alternative["capabilities"]
        needs = alternative.get("needs", [])
        if needs:
            result["needs"] = [
                {key: value for key, value in need.items() if key in {
                    "equipmentId", "quantity", "minMassGrams", "maxMassGrams", "suitability"
                }}
                for need in needs
            ]
        sets.append(result)
    return sets or [{}]


def _exercise(exercise: dict[str, Any]) -> dict[str, Any]:
    # Runtime fields only: Blender-only pose/prop metadata is retained in the
    # sidecar, where pose tools can read it without weakening strict parsing.
    result = {
        "id": exercise["id"],
        "familyId": exercise["familyId"],
        "name": exercise["name"],
        "patterns": exercise["patterns"],
        "areas": exercise.get("areas", []),
        "stretchAreas": exercise.get("stretchAreas", []),
        "kind": exercise["kind"],
        "equipmentAlternatives": _requirement_sets(exercise),
        "unilateral": exercise.get("unilateral", False),
        "position": exercise.get("position", "floor"),
        "instructions": exercise.get("instructions", []),
        "formCues": exercise.get("formCues", []),
        "cautions": exercise.get("cautions", []),
        "difficultyRank": exercise.get("difficultyRank") or 0,
        "sourceIds": exercise.get("sourceIds", []),
        "reviewState": exercise.get("reviewState", "DRAFT"),
        "primaryMuscles": exercise.get("primaryMuscles", []),
        "secondaryMuscles": exercise.get("secondaryMuscles", []),
    }
    if exercise["kind"] in {"REPS", "HOLD"}:
        result["progressionPolicyId"] = "pol-" + exercise["id"]
        result["compatibleStretchIds"] = []
    if exercise["kind"] in {"STRETCH", "MOBILITY"}:
        result["defaultSeconds"] = exercise.get("defaultSeconds", 30)
    if exercise.get("cautionStrong"):
        result["cautionStrong"] = True
    if exercise.get("routineOnly"):
        result["routineOnly"] = True
    return result


MUSCLE_STRETCH_AREAS = {
    "CHEST": {"CHEST"},
    "FRONT_DELTS": {"SHOULDER"}, "SIDE_DELTS": {"SHOULDER"}, "REAR_DELTS": {"SHOULDER"},
    "TRICEPS": {"SHOULDER"}, "BICEPS": {"SHOULDER"}, "FOREARMS": {"WRIST"},
    "LATS": {"BACK", "SHOULDER"}, "UPPER_BACK": {"BACK", "SHOULDER"}, "LOWER_BACK": {"BACK", "HIP"},
    "ABS": {"BACK"}, "OBLIQUES": {"BACK", "HIP"}, "GLUTES": {"HIP"},
    "QUADS": {"QUAD", "HIP"}, "HAMSTRINGS": {"HAMSTRING"}, "CALVES": {"CALF", "ANKLE"},
    "TIBIALIS": {"CALF", "ANKLE"}, "HIP_FLEXORS": {"HIP"}, "ADDUCTORS": {"HIP"},
    "NECK": {"SHOULDER"},
}


def _link_compatible_stretches(catalog: dict[str, Any], new_ids: set[str]) -> None:
    """Link each imported strength row to up to three relevant catalog stretches."""
    variations = {v["id"]: v for v in catalog["variations"]}
    stretches = [v for v in catalog["variations"] if v["kind"] == "STRETCH"]
    for variation_id in new_ids:
        variation = variations[variation_id]
        if variation["kind"] not in {"REPS", "HOLD"}:
            continue
        primary = set(variation.get("primaryMuscles", []))
        secondary = set(variation.get("secondaryMuscles", []))
        wanted = set().union(*(MUSCLE_STRETCH_AREAS.get(m, set()) for m in primary | secondary))
        scored = []
        for stretch in stretches:
            stretch_areas = set(stretch.get("stretchAreas", []))
            score = len(wanted & stretch_areas)
            # A primary-muscle match wins ties, then stable id ordering keeps
            # generated assets byte-for-byte deterministic.
            if score:
                primary_areas = set().union(*(MUSCLE_STRETCH_AREAS.get(m, set()) for m in primary))
                primary_match = len(primary_areas & stretch_areas)
                scored.append((score, primary_match, stretch["id"]))
        ranked = sorted(scored, key=lambda row: (-row[0], -row[1], row[2]))
        variation["compatibleStretchIds"] = [row[2] for row in ranked[:3]]
        # Every imported strength drill needs an automatic, gentle option for
        # between-set recovery. The most relevant stretch matches can all be
        # deep cool-down holds (notably a wall sit's hip/quadriceps matches),
        # so append the strongest safe, non-opt-in option when necessary.
        safe = [row for row in ranked if variations[row[2]].get("stretchUse") != "COOL_DOWN"
                and not variations[row[2]].get("optIn", False)]
        if safe and not any(variations[sid].get("stretchUse") != "COOL_DOWN"
                            and not variations[sid].get("optIn", False)
                            for sid in variation["compatibleStretchIds"]):
            variation["compatibleStretchIds"].append(safe[0][2])
        if not variation["compatibleStretchIds"] and "stretch-back" in variations:
            # The contract has one drill with no muscle map (forward shoulder
            # roll); the general back stretch is the safe DRAFT fallback.
            variation["compatibleStretchIds"] = ["stretch-back"]

    # Keep every authored stretch reachable. A top-three limit can otherwise
    # leave a narrow area (e.g. groin/adductors) orphaned when several general
    # hip stretches score equally well. Attach each orphan to its strongest
    # matching strength exercise; ties resolve by stable id ordering.
    strength = [v for v in variations.values() if v["kind"] in {"REPS", "HOLD"}]
    linked = {sid for v in strength for sid in v.get("compatibleStretchIds", [])}
    for stretch in stretches:
        if stretch["id"] in linked:
            continue
        target_areas = set(stretch.get("stretchAreas", []))
        choices = []
        for v in strength:
            muscles = set(v.get("primaryMuscles", [])) | set(v.get("secondaryMuscles", []))
            areas = set().union(*(MUSCLE_STRETCH_AREAS.get(m, set()) for m in muscles))
            overlap = len(areas & target_areas)
            if overlap:
                primary = set().union(*(MUSCLE_STRETCH_AREAS.get(m, set()) for m in v.get("primaryMuscles", [])))
                choices.append((overlap, len(primary & target_areas), v["id"]))
        if choices:
            best = sorted(choices, key=lambda row: (-row[0], -row[1], row[2]))[0][2]
            variations[best]["compatibleStretchIds"].append(stretch["id"])
            linked.add(stretch["id"])


def _attach_aliases(catalog: dict[str, Any], contract: dict[str, Any]) -> None:
    by_id = {variation["id"]: variation for variation in catalog["variations"]}
    for alias in contract.get("aliases", []):
        target = by_id.get(alias["existingId"])
        if target is None:
            raise ValueError(f"alias target missing: {alias['existingId']}")
        values = target.setdefault("aliases", [])
        if alias["alias"] not in values:
            values.append(alias["alias"])


def _reviewed_routes(catalog: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    """Materialize explicit reviewed paths and role memberships for every ID."""
    ids = {v["id"] for v in catalog["variations"]}
    routes: list[dict[str, Any]] = []
    primary_membership: dict[str, dict[str, Any]] = {}
    classified: dict[str, list[dict[str, Any]]] = defaultdict(list)
    route_by_id = {r["id"]: r for r in ROUTES}
    if len(route_by_id) != len(ROUTES):
        raise ValueError("duplicate progression route id")
    for authored in ROUTES:
        path = authored["mainPath"].split()
        unknown = set(path) - ids
        if unknown:
            raise ValueError(f"route {authored['id']} references missing main-path IDs: {sorted(unknown)}")
        route = {k: v for k, v in authored.items() if k != "mainPath"}
        route["mainPath"] = path
        route["members"] = [{"variationId": item, "role": "MAIN"} for item in path]
        for item in path:
            classified[item].append({"variationId": item, "role": "MAIN"})
            primary_membership.setdefault(item, route)
        routes.append(route)
    for variation_id, (route_id, role, anchor) in EXTRAS.items():
        if variation_id not in ids:
            raise ValueError(f"explicit role mapping references missing ID: {variation_id}")
        route = route_by_id.get(route_id)
        if route is None:
            raise ValueError(f"explicit role mapping references missing route: {route_id}")
        member = {"variationId": variation_id, "role": role}
        if anchor:
            if anchor not in ids:
                raise ValueError(f"{variation_id}: unknown route anchor {anchor}")
            member["atVariationId"] = anchor
        target = next(r for r in routes if r["id"] == route_id)
        if not any(m["variationId"] == variation_id and m["role"] == role for m in target["members"]):
            target["members"].append(member)
        classified[variation_id].append(member)
        primary_membership.setdefault(variation_id, target)
    for variation_id, route_id, role, anchor in EXTRA_MEMBERSHIPS:
        if variation_id not in ids:
            raise ValueError(f"multi-route role mapping references missing ID: {variation_id}")
        target = next((r for r in routes if r["id"] == route_id), None)
        if target is None:
            raise ValueError(f"multi-route role mapping references missing route: {route_id}")
        if anchor and anchor not in ids:
            raise ValueError(f"{variation_id}: unknown cross-route anchor {anchor}")
        member = {"variationId": variation_id, "role": role}
        if anchor:
            member["atVariationId"] = anchor
        prior = next((m for m in target["members"] if m["variationId"] == variation_id), None)
        if prior is not None and prior != member:
            raise ValueError(f"{variation_id}: conflicting membership in route {route_id}")
        if prior is None:
            target["members"].append(member)
        classified[variation_id].append(member)
        primary_membership.setdefault(variation_id, target)
    condition_route = next(r for r in routes if r["id"] == "conditioning-pool")
    for variation_id in sorted(CONDITIONING_POOL_IDS):
        if variation_id not in ids:
            raise ValueError(f"conditioning pool references missing ID: {variation_id}")
        member = {"variationId": variation_id, "role": "LIBRARY"}
        condition_route["members"].append(member)
        classified[variation_id].append(member)
        primary_membership.setdefault(variation_id, condition_route)
    unassigned = ids - set(classified)
    if unassigned != EXPLICIT_LIBRARY_IDS:
        raise ValueError(f"explicit library-role table mismatch; missing={sorted(unassigned-EXPLICIT_LIBRARY_IDS)}, stale={sorted(EXPLICIT_LIBRARY_IDS-unassigned)}")
    # Deliberately classify the explicit table as library-only.
    library_members = []
    for variation_id in sorted(EXPLICIT_LIBRARY_IDS):
        member = {"variationId": variation_id, "role": "LIBRARY"}
        library_members.append(member)
        classified[variation_id].append(member)
    library_route = {"id": "library", "name": "Exercise library", "progressionGroup": "library",
                     "mainPath": [], "members": library_members, "defaultPool": False, "goalIds": []}
    routes.append(library_route)
    for variation_id, rows in classified.items():
        # Multi-route exercises have one stable variation-level group, while
        # route membership remains the source of route-specific roles.
        primary = primary_membership.get(variation_id, library_route)
        primary_membership[variation_id] = primary
    if set(classified) != ids:
        raise ValueError(f"route coverage mismatch: {sorted(ids - set(classified))}")
    return routes, primary_membership


def _role_counts(routes: list[dict[str, Any]]) -> dict[str, int]:
    roles: dict[str, set[str]] = defaultdict(set)
    for route in routes:
        for member in route["members"]:
            roles[member["role"]].add(member["variationId"])
    return {role: len(ids) for role, ids in sorted(roles.items())}


def _route_edge_audit(routes: list[dict[str, Any]], review: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, str]]]:
    route_by_id = {route["id"]: route for route in routes}
    audit = []
    unresolved = []
    for row in review:
        if not row["reviewRequired"]:
            continue
        children = []
        for child_id in row["newChildren"]:
            disposition = "unresolved"
            evidence = []
            for route in routes:
                path = route["mainPath"]
                for parent, next_id in zip(path, path[1:]):
                    if parent == row["parentId"] and next_id == child_id:
                        disposition = "main-step"
                        evidence.append(route["id"])
                member = next((m for m in route["members"] if m["variationId"] == child_id), None)
                if member and member["role"] in {"ALTERNATE", "SUPPORT"} and member.get("atVariationId") == row["parentId"]:
                    disposition = member["role"].lower() + "-at-parent"
                    evidence.append(route["id"])
            if disposition == "unresolved":
                child_roles = [m["role"] for route in routes for m in route["members"] if m["variationId"] == child_id]
                if "PROGRAM" in child_roles:
                    disposition = "program-only"
                elif "LIBRARY" in child_roles:
                    disposition = "library-only"
                elif "MAIN" in child_roles:
                    disposition = "main-step-on-separate-route"
                elif child_roles:
                    disposition = "explicitly-reanchored"
            children.append({"id": child_id, "disposition": disposition, "routeIds": sorted(set(evidence))})
            if disposition == "unresolved":
                unresolved.append({"parentId": row["parentId"], "childId": child_id})
        audit.append({"parentId": row["parentId"], "children": children})
    return audit, unresolved


def _apply_route_main_successors(catalog: dict[str, Any], routes: list[dict[str, Any]]) -> None:
    """Reduce route nodes to reviewed spine edges; route paths handle splits."""
    successors: dict[str, set[str]] = defaultdict(set)
    route_nodes = set()
    for route in routes:
        path = route["mainPath"]
        route_nodes.update(path)
        for parent, child in zip(path, path[1:]):
            successors[parent].add(child)
    policies = {p["variationId"]: p for p in catalog["policies"]}
    for parent in route_nodes:
        policy = policies.get(parent)
        if policy is None:
            continue
        children = successors.get(parent, set())
        if children:
            policy["nextVariationIds"] = sorted(children)
        else:
            policy.pop("nextVariationIds", None)
        if len(children) == 1:
            policy["mainSuccessorId"] = next(iter(children))
        else:
            policy.pop("mainSuccessorId", None)


def _apply_route_prerequisites(catalog: dict[str, Any], routes: list[dict[str, Any]]) -> None:
    """Gate authored main-path advances on the immediately preceding mastery.

    Multiple route entries for one child are represented as one OR group;
    independent compound gates remain separate AND groups. Route-specific
    readiness thresholds are separate from automatic mastery at tier five.
    """
    policies = {p["variationId"]: p for p in catalog["policies"]}
    for variation_id in ("pushup-archer", "nordic-negative-banded"):
        if variation_id in policies:
            policies[variation_id].pop("prerequisiteRule", None)
    incoming: dict[str, dict[str, int]] = defaultdict(dict)
    for route in routes:
        path = route["mainPath"]
        for parent, child in zip(path, path[1:]):
            incoming[child][parent] = ROUTE_GATE_TIERS[route["id"]]

    for child, parents in incoming.items():
        policy = policies.get(child)
        if policy is None:
            continue
        prior_groups = list(policy.get("prerequisiteRule", {}).get("allOf", []))
        parent_ids = set(parents)
        existing_thresholds = {}
        route_parent_alternatives = set()
        independent_groups = []
        for group in prior_groups:
            matching = []
            for pred in group:
                gate = pred.get("variationTierMet")
                if gate and gate["variationId"] in parent_ids:
                    existing_thresholds[gate["variationId"]] = gate["tier"]
                    matching.append(gate["variationId"])
            if matching and len(group) > len(matching):
                # Preserve a reviewed OR entry such as band row OR towel row;
                # its main-path parent is already represented in that clause.
                route_parent_alternatives.update(matching)
                independent_groups.append(group)
            elif matching:
                # Pure predecessor clauses can be folded into the route-local
                # OR group below while retaining their reviewed thresholds.
                continue
            else:
                independent_groups.append(group)
        route_group = [
            {"variationTierMet": {
                "variationId": parent,
                "tier": existing_thresholds.get(parent, gate_tier),
            }}
            for parent, gate_tier in sorted(parents.items()) if parent not in route_parent_alternatives
        ]
        policy["prerequisiteRule"] = {"allOf": independent_groups + ([route_group] if route_group else [])}

    # The bar muscle-up keeps its compound chest-to-bar + straight-bar-dip
    # readiness gate in addition to the route's immediately previous negative.
    muscle_up = policies.get("muscle-up-bar")
    if muscle_up is not None:
        rule = muscle_up.setdefault("prerequisiteRule", {"allOf": []})
        dip_predicate = {"variationTierMet": {"variationId": "dip-straight-bar", "tier": 4}}
        if not any(dip_predicate in group for group in rule["allOf"]):
            rule["allOf"].append([dip_predicate])


def _routine(routine: dict[str, Any]) -> dict[str, Any]:
    items = []
    for item in routine["items"]:
        items.append({
            key: item[key] for key in ("exerciseId", "reps", "seconds", "perSide", "estSeconds", "notes")
            if key in item
        })
    return {
        "id": routine["id"],
        "name": routine["name"],
        "source": routine.get("source", ""),
        "flow": routine.get("flow", False),
        "items": items,
        "notes": routine.get("notes", ""),
    }


def _contract_metadata(contract: dict[str, Any]) -> dict[str, Any]:
    # Keep all render-specific fields too; the overnight clip pipeline uses
    # this alongside the small runtime catalog.
    return {
        "equipment": contract.get("equipmentNew", []),
        "sources": contract.get("sourcesNew", []),
        "families": contract.get("familiesNew", []),
        "tabs": contract.get("tabsNew", []),
        "tabsExtended": contract.get("tabsExtended", []),
        "poseGroups": contract.get("poseGroups", {}),
        "exerciseDetails": [
            {key: exercise[key] for key in (
                "id", "sourceRefs", "poseGroup", "props", "renderBatch", "gapTier", "clipKind",
                "insertAfter", "alsoRequires", "routineOnly", "addWithCaution", "cautionStrong",
                "defaultSeconds", "note",
            ) if key in exercise}
            for exercise in contract.get("exercises", [])
        ],
        # Provenance stays intact in this asset even though the runtime alias
        # field contains only display strings.
        "aliases": contract.get("aliases", []),
    }


def _validate_contract(base: dict[str, Any], contract: dict[str, Any]) -> None:
    existing_ids = {v["id"] for v in base["variations"]}
    new_exercises = contract.get("exercises", [])
    new_ids = [v["id"] for v in new_exercises]
    if len(new_ids) != len(set(new_ids)):
        raise ValueError("duplicate new exercise id in contract")
    overlap = existing_ids.intersection(new_ids)
    if overlap:
        raise ValueError(f"R8 ids already exist in base catalog: {sorted(overlap)}")
    all_ids = existing_ids | set(new_ids)
    base_families = {v["familyId"] for v in base["variations"]}
    new_families = {f["familyId"] for f in contract.get("familiesNew", [])}
    unknown_families = sorted({v["familyId"] for v in new_exercises} - base_families - new_families)
    if unknown_families:
        raise ValueError(f"exercise family not declared: {unknown_families}")

    source_ids = {s for v in base["variations"] for s in v.get("sourceIds", [])}
    source_ids.update(source["id"] for source in contract.get("sourcesNew", []))
    for exercise in new_exercises:
        if exercise["kind"] in {"REPS", "HOLD"}:
            for source_id in exercise.get("sourceIds", []):
                if source_id not in source_ids:
                    raise ValueError(f"{exercise['id']}: unknown source id {source_id}")
        for item in exercise.get("equipment", [{}]):
            for need in item.get("needs", []):
                equipment_id = need["equipmentId"]
                if equipment_id not in _known_equipment_ids(base, contract):
                    raise ValueError(f"{exercise['id']}: unknown equipment id {equipment_id}")

    for exercise in new_exercises:
        _policy(exercise, all_ids) if exercise["kind"] in {"REPS", "HOLD"} else None
    for routine in contract.get("routines", []):
        for item in routine["items"]:
            if item["exerciseId"] not in all_ids:
                raise ValueError(f"routine {routine['id']} references unknown exercise {item['exerciseId']}")


def _known_equipment_ids(base: dict[str, Any], contract: dict[str, Any]) -> set[str]:
    known = {item["id"] for item in contract.get("equipmentNew", [])}
    for variation in base["variations"]:
        for alternative in variation.get("equipmentAlternatives", []):
            known.update(need["equipmentId"] for need in alternative.get("needs", []))
    return known


def _rank(exercise: dict[str, Any] | None) -> int | None:
    if exercise is None:
        return None
    value = exercise.get("difficultyRank")
    return value if isinstance(value, int) else None


def _merge_successors(base: dict[str, Any], contract: dict[str, Any]) -> list[dict[str, Any]]:
    """Retain old edges, add contract edges, and write mainline review choices."""
    by_variation = {v["id"]: v for v in base["variations"]}
    policies = {p["variationId"]: p for p in base["policies"]}
    children: dict[str, list[str]] = defaultdict(list)
    for exercise in contract.get("exercises", []):
        parent = exercise.get("insertAfter")
        if parent:
            children[parent].append(exercise["id"])
    review = []
    for parent, child_ids in children.items():
        policy = policies.get(parent)
        if policy is None:
            parent_variation = by_variation.get(parent)
            if parent_variation is None:
                raise ValueError(f"insertAfter {parent} is not a catalog variation")
            # Stretch/mobility insertAfter links describe ordering within their
            # category; they do not have app progression policies.
            if parent_variation["kind"] not in {"REPS", "HOLD"}:
                continue
            raise ValueError(f"insertAfter {parent} has no progression policy")
        existing = list(policy.get("nextVariationIds", []))
        # Avoid changing any pre-R8 next edge or its order. New branches are
        # retained as next links so the tree can show them.
        policy["nextVariationIds"] = existing + [child for child in child_ids if child not in existing]
        existing_main = policy.get("mainSuccessorId") or (existing[0] if existing else None)
        chosen_main = existing_main
        if chosen_main:
            policy["mainSuccessorId"] = chosen_main

        main_exercise = by_variation.get(existing_main) if existing_main else None
        insertion_candidates = [
            child for child in child_ids
            if main_exercise is not None
            and by_variation[child]["familyId"] == by_variation[parent]["familyId"]
            and by_variation[child]["kind"] == by_variation[parent]["kind"]
            and _rank(by_variation[child]) is not None and _rank(main_exercise) is not None
            and _rank(by_variation[child]) < _rank(main_exercise)
        ]
        review.append({
            "parentId": parent,
            "existingNextVariationIds": existing,
            "newChildren": child_ids,
            "selectedMainSuccessorId": chosen_main,
            "selectionReason": "preserved-existing-main" if existing_main else "no-reviewed-route-successor",
            "lowerRankInsertionCandidates": insertion_candidates,
            "reviewRequired": bool(insertion_candidates) or len(child_ids) > 1 or existing_main is not None,
        })
    for policy in base["policies"]:
        next_ids = policy.get("nextVariationIds", [])
        if next_ids:
            policy.setdefault("mainSuccessorId", next_ids[0])

    # An owner-confirmed comparable group overrides the legacy first-edge
    # default at its unique common parent. The alternative edge remains in
    # nextVariationIds for the tree and rotation logic.
    for group in CONFIRMED_EXERCISE_GROUPS:
        members = {group["mainVariationId"], *group["alternativeVariationIds"]}
        parents = [p for p in base["policies"] if members.issubset(set(p.get("nextVariationIds", [])))]
        if len(parents) != 1:
            raise ValueError(f"comparable group {group['id']} must resolve to one common progression parent; found {len(parents)}")
        parent_policy = parents[0]
        parent_policy["mainSuccessorId"] = group["mainVariationId"]
        parent_id = parent_policy["variationId"]
        for row in review:
            if row["parentId"] == parent_id:
                row["selectedMainSuccessorId"] = group["mainVariationId"]
                row["selectionReason"] = "owner-confirmed-comparable-exercise-group"
                row["reviewRequired"] = True
    return sorted(review, key=lambda item: item["parentId"])


def import_contract(base: dict[str, Any], contract: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], list[dict[str, Any]]]:
    """Pure import used by the CLI and fixture tests."""
    _validate_contract(base, contract)
    result = json.loads(json.dumps(base))
    result["variations"].extend(_exercise(exercise) for exercise in contract["exercises"])
    known_ids = {v["id"] for v in result["variations"]}
    _link_compatible_stretches(result, set(e["id"] for e in contract["exercises"]))
    result["policies"].extend(
        _policy(exercise, known_ids) for exercise in contract["exercises"]
        if exercise["kind"] in {"REPS", "HOLD"}
    )

    _attach_aliases(result, contract)
    review = _merge_successors(result, contract)

    # A few id aliases are already in the contract list; apply them before
    # writing the catalog so users can search/display those names immediately.
    result["fixedPrograms"] = [_routine(routine) for routine in contract.get("routines", [])]
    result["exerciseGroups"] = json.loads(json.dumps(REVIEWED_ROTATION_GROUPS))
    result["optionSets"] = contract.get("options", [])
    routes, primary_routes = _reviewed_routes(result)
    result["progressionRoutes"] = routes
    _apply_route_main_successors(result, routes)
    _apply_route_prerequisites(result, routes)
    updated_policies = {p["variationId"]: p for p in result["policies"]}
    route_next: dict[str, set[str]] = defaultdict(set)
    for route in routes:
        for parent, child in zip(route["mainPath"], route["mainPath"][1:]):
            route_next[parent].add(child)
    for row in review:
        choices = route_next.get(row["parentId"], set())
        policy = updated_policies.get(row["parentId"], {})
        row["selectedMainSuccessorId"] = policy.get("mainSuccessorId")
        row["selectionReason"] = ("route-specific-successors" if len(choices) > 1 else
                                   "reviewed-route" if choices else "no-reviewed-route-successor")
    for variation in result["variations"]:
        route = primary_routes[variation["id"]]
        variation["progressionGroup"] = PROGRESSION_GROUP_OVERRIDES.get(
            variation["id"], None if route["id"] == "library" else route["progressionGroup"]
        )
        if variation["id"] in PREHAB_IDS:
            variation["progressionGroup"] = "prehab"
        variation["demonstrationHold"] = variation["id"] in DEMONSTRATION_HOLD

    # Preserve both the prior and newly authored metadata in the standalone
    # sidecar. These structures are consumed by the tabs/equipment and pose work.
    metadata = _contract_metadata(contract)

    prior_clip_meta = json.loads(CLIP_META.read_text()) if CLIP_META.exists() else {}
    clip_meta = dict(prior_clip_meta)
    for exercise in contract["exercises"]:
        clip_meta[exercise["id"]] = {
            "kind": exercise["clipKind"],
            "primary": exercise.get("primaryMuscles", []),
            "secondary": exercise.get("secondaryMuscles", []),
            "sharedPoseWith": [],
        }
    # Entries must exactly follow catalog ids: retaining an orphan here would
    # fail the same strict app asset test that protects rendered clips.
    clip_meta = {variation["id"]: clip_meta[variation["id"]] for variation in result["variations"] if variation["id"] in clip_meta}
    if set(clip_meta) != known_ids:
        missing = sorted(known_ids - set(clip_meta))
        raise ValueError(f"clip_meta has no source muscles/kind for catalog ids: {missing}")
    return result, metadata, clip_meta, review


def _write_json(path: pathlib.Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    # Compact JSON is deliberately used for the expanded catalog so each
    # shipped/source copy remains below tools/preflight.py's 1 MiB file limit.
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n")


def main() -> int:
    try:
        subprocess.run([sys.executable, str(BASE_GENERATOR)], cwd=ROOT, check=True)
        base = json.loads(CONTENT_CATALOG.read_text())
        contract = json.loads(CONTRACT.read_text())
        result, metadata, clip_meta, review = import_contract(base, contract)
        _write_json(CONTENT_CATALOG, result)
        _write_json(ASSET_CATALOG, result)
        _write_json(CONTENT_META, metadata)
        _write_json(ASSET_META, metadata)
        _write_json(CLIP_META, clip_meta)
        contract = json.loads(CONTRACT.read_text())
        by_policy = {p["variationId"]: p for p in result["policies"]}
        target_exceptions = []
        for exercise in contract["exercises"]:
            if exercise["kind"] not in {"REPS", "HOLD"}:
                continue
            targets = [t["target"]["value"] for t in by_policy[exercise["id"]]["tiers"]]
            default = exercise.get("defaultSeconds")
            if exercise["kind"] == "HOLD" and default is not None:
                target_exceptions.append(f'{exercise["id"]}: default {default}s -> {targets}s')
            elif exercise["familyId"] in POWER_FAMILIES:
                target_exceptions.append(f'{exercise["id"]}: quality profile -> {targets} reps')
        _write_json(REVIEW_REPORT, {
            "progressionEdges": review,
            "targetProfileExceptions": target_exceptions,
            "unresolvedEdgeCount": sum(1 for row in review if row["reviewRequired"]),
        })
        edge_audit, unresolved_edges = _route_edge_audit(result["progressionRoutes"], review)
        reviewed_edge_count = sum(len(row["children"]) for row in edge_audit)
        _write_json(ROUTE_REPORT, {
            "status": "DRAFT",
            "source": "tools/progression_routes.py",
            "variationCount": len(result["variations"]),
            "routeCount": len(result["progressionRoutes"]),
            "roleCounts": _role_counts(result["progressionRoutes"]),
            "reviewedParentCount": len(edge_audit),
            "reviewedEdgeCount": reviewed_edge_count,
            "edgeAudit": edge_audit,
            "unresolvedEdges": unresolved_edges,
            "notes": [
                "Each child of the 47 review-required legacy parents is classified by its authored route role and anchor.",
                "Only pairs without an authored main step, alternate/support anchor, program/library role, or explicit reanchor remain unresolved.",
                "Route roles are explicitly authored in the route table; unmatched entries are explicitly listed as library-only.",
            ],
        })
        print(f"wrote {len(result['variations'])} variations, {len(result['policies'])} policies, {len(result['fixedPrograms'])} programs")
        for path in (CONTENT_CATALOG, ASSET_CATALOG, CONTENT_META, ASSET_META, CLIP_META, REVIEW_REPORT, ROUTE_REPORT):
            print(f"{path.relative_to(ROOT)}: {path.stat().st_size} bytes")
        return 0
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as exc:
        print(f"R8 catalog import failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
