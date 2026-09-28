# Calisthenics App: Product Idea and Landscape Survey

**Prepared:** 2026-09-24
**Purpose:** Capture the feature concept and the initial open-source/commercial app survey before implementation begins.

## 1. The product idea

The starting point is a basic interval timer and a mostly repeated set of exercises. The proposed app should make training more progressive, adaptable and varied without losing the simplicity of a guided workout.

### Desired features

1. **Progression that responds to ability** — change or recommend exercises as the user reaches a new level, instead of repeating the same routine indefinitely.
2. **Skill-oriented progression paths** — show how foundational exercises build toward calisthenics skills, with a visible progression tree or prerequisite map.
3. **Equipment-aware programming** — generate or adapt sessions based on available equipment, with travel especially in mind (for example, quickly switching from home equipment to no equipment or a hotel/park setup).
4. **Optional stretches during a workout** — offer stretching between exercises, with full-body stretches as the default and the ability to focus on selected body parts.
5. **Open-source foundation** — the project is intended to be open source; the survey was meant to identify existing products and reusable ideas before building from scratch.

## 2. Findings at a glance

**The closest existing commercial product is Calistree.** It combines skill trees, automatic progression, equipment-aware programs, workout tracking and a broad library that includes mobility and stretching.[5][18]

Fitloop is another close commercial/freemium match, with progression ladders, equipment-aware plans and mobility content.[17][19][20]

**No mature, clearly open-source app found in this survey combines all the requested pieces.** Ironvellum is the most relevant open-source project to inspect first: it has an extensive skill tree and an editable starter week based on goals and equipment.[12]

**The most apparent gap is not simply “an app with stretches.”** Several apps include flexibility or mobility routines. I did not find an explicit feature description for automatically inserting targeted stretches between strength exercises, with full body as the default and user-selectable body-area focus.[6][8][11]

This was a broad discovery survey across GitHub, F-Droid, publisher sites and app-store listings, not a claim that every app or repository ever made has been located. Projects without a verifiable software license are not counted here as confirmed open source.

## 3. Feature categories in the market

| Category | What it covers | Market finding |
|---|---|---|
| Skill library and progression map | Exercise variants, prerequisites, paths to skills | Available in several commercial apps and some open-source projects. |
| Adaptive progression | Recommending harder/easier exercises or changing plans as performance changes | Offered commercially, but the way progression is decided is often product-specific. |
| Equipment-aware workout planning | Filtering or generating work around available equipment | A strong feature in some apps; travel-friendly, quickly switchable equipment profiles are less universal. |
| Workout execution and tracking | Timers, sets/reps/holds, session history and routine builders | Common and well-established; likely not a reason to rebuild from zero. |
| Mobility and stretching | Warm-ups, cooldowns, flexibility or mobility sessions | Widely available as separate content; automatic, targeted interleaving during strength workouts was not verified. |

## 4. Open-source and adjacent projects

| Project | License/status | What it offers | Main gap relative to the idea |
|---|---|---|---|
| [Ironvellum](https://github.com/AlexMollard/Ironvellum) | GPL-3.0-or-later | Android/offline-first tracker; 106-technique tree across calisthenics and mobility; prerequisite gates; first-run editable training week built from goals and equipment.[12] | Does not document the proposed travel-profile switching or interleaved-stretch workflow. Closest architecture to investigate. |
| [Calistenia](https://github.com/guillermoscript/calistenia-app) | MIT | Web PWA and mobile project; phased training, session logging, timers, circuits, AI coach; repository notes exercise-level equipment tags and filters.[1][23] | No advertised visual skill prerequisite tree or contextual stretch insertion. |
| [Calisthenics Memory](https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/) | GPL-3.0-only | Active Android tracker; custom exercises/programs, intervals, timers, history and user-defined 10-level progressions.[2] | Flexible manual tracking rather than an adaptive coach selecting the next progression. |
| [Ballast](https://github.com/N-O-P-E/Ballast) | MIT | Offline PWA tracker with editable progression steps for 15 skills and workout templates.[3] | Tracking and templates rather than adaptive workout generation. |
| [Cali Ascension](https://github.com/Alex-0234/cali-ascension) | AGPL | Early gamified web tracker with skill-tree unlocks and progression based on logged reps.[21] | Early project; not a mature equipment-aware programming engine. |
| [Krida](https://github.com/SirPuech/krida-calisthenics-journey) | License not verified from repository page | Static guide with 111 skills, prerequisites and form demonstrations.[4] | Not an adaptive workout app; not counted here as confirmed FOSS without a verifiable license. |

## 5. Commercial apps and relevant strengths

| App | Relevant features | Notes against the requested workflow |
|---|---|---|
| [Calistree](https://calistree.com) | Skill trees, automatic progression, equipment-aware personalized plans, exercise/routine library with mobility and stretching.[5][6] | Closest overall. The current official pricing page lists 1,700+ exercises. Free use includes unlimited workout sessions but limits some objects (including equipment profiles, routines and plans); its US page lists $5.99/month, $44.99/year or $179.99 lifetime. Prices vary by region.[18] |
| [Fitloop](https://fitloop.app/) | Skill/progression ladders, plans using goals and equipment, mobility routines; core features are free and some AI/advanced plan features are paid.[17][19][20] | Strong second comparison, though no explicit automatic between-exercise stretch mode was found. |
| [Calisteniapp](https://calisteniapp.com/) | Skill-specific programs, adaptive EVO routines, workout creator, full-body stretching/flexibility content.[7][8][15] | Strong content breadth; stretching appears as routines/programs rather than documented automatic interleaving. |
| [Thenics](https://www.thenics.de/) | Step-by-step skill progressions and adaptive workouts; personalized Coach is a Pro feature.[9][13] | Focused on bodyweight skills; less clearly an equipment-profile travel planner. |
| [DIE RINGE](https://dieringe.com/) | Personalized, stepwise plans and a large exercise library.[10] | Particularly oriented around gymnastics rings. |
| [Caliverse](https://play.google.com/store/apps/details?id=com.caliverseapps.caliverse) | Large library of exercises/workouts; Pro includes progressive plans for specific skills.[14] | Less emphasis in its published description on a visible skill tree or detailed equipment profiles. |
| [Freeletics](https://www.freeletics.com/en/bodyweight-training/) | Adaptive bodyweight sessions; users can adjust Coach-assigned sessions.[16][22] | Broader bodyweight/fitness coach, less centered on calisthenics prerequisite trees. |
| [GMB Elements](https://gmb.io/e/) | Structured movement progressions and mobility-focused training.[11] | Program/course approach, not a general-purpose skill-tree workout generator. |

## 6. Product implications

- The overall product concept already exists commercially; **Calistree and Fitloop overlap substantially** with the progression, skills and equipment-planning parts.
- The most credible differentiator is a **genuinely open-source/self-hostable app** with reliable travel-equipment profiles and an explicit, configurable **interleaved-stretch mode**.
- Avoid recreating commodity features such as interval timers, session logging and routine builders unless they are needed to support that differentiator.
- A sensible next validation step is to try Calistree against the current routine, then inspect Ironvellum and Calistenia to decide whether adapting an existing codebase is more efficient than starting clean.

## Sources

[1] https://github.com/guillermoscript/calistenia-app — Calistenia App repository
[2] https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory — Calisthenics Memory F-Droid
[3] https://github.com/N-O-P-E/Ballast — Ballast repository
[4] https://github.com/SirPuech/krida-calisthenics-journey — Krida repository
[5] https://calistree.com — Calistree official site
[6] https://calistree.app/routines — Calistree routines
[7] https://calisteniapp.com — Calisteniapp official site
[8] https://calisteniapp.com/routines/10_minute_full_body_stretching/SR505 — Calisteniapp stretching routine
[9] https://www.thenics.de — Thenics official site
[10] https://dieringe.com — DIE RINGE official site
[11] https://gmb.io/e — GMB Elements
[12] https://github.com/AlexMollard/Ironvellum — Ironvellum repository
[13] https://play.google.com/store/apps/details?id=com.abad.thenics — Thenics Google Play
[14] https://play.google.com/store/apps/details?id=com.caliverseapps.caliverse — Caliverse Google Play
[15] https://calisteniapp.com/workouts/programs — Calisteniapp programs
[16] https://www.freeletics.com/en/bodyweight-training — Freeletics bodyweight
[17] https://www.fitloop.app — Fitloop official site
[18] https://calistree.com/pricing — Calistree pricing
[19] https://apps.apple.com/us/app/fitloop-calisthenics-gym/id1474941254 — Fitloop App Store
[20] https://fitloop.app/progressions — Fitloop ladders
[21] https://github.com/Alex-0234/cali-ascension — Cali Ascension
[22] https://help.freeletics.com/hc/en-us/articles/360003933780-Adapt-your-Bodyweight-training-session — Freeletics session adaptation
[23] https://raw.githubusercontent.com/guillermoscript/calistenia-app/main/FEATURE_PRIORITIES.md — Calistenia equipment notes
