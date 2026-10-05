package app.calisthenics.domain

import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ProfileEditTest {
    private val catalog = parseCatalog(File(System.getProperty("repo.root"), "content/starter/catalog.json").readText())

    @Test fun seedProfilesRoundTripThroughTheChecklist() {
        for (p in SeedProfiles.all) {
            val back = buildProfile(p.id, p.name, selectionOf(p))
            assertEquals(p.items.sortedBy { it.equipmentId }, back.items.sortedBy { it.equipmentId })
            assertEquals(p.capabilities, back.capabilities)
        }
    }

    @Test fun everyCatalogEquipmentIdHasAChecklistRow() {
        val needed = catalog.variations.flatMap { it.equipmentAlternatives }.flatMap { it.needs }.map { it.equipmentId }.toSet()
        for (id in needed) assertNotNull("no checklist row for $id", EquipmentOptions.byId(id))
    }

    @Test fun muscleUpIsLockedWithoutAHighBarAndUnlocksWithOne() {
        val mu = catalog.variation("muscle-up-bar")!!
        val gym = buildProfile("gym", "Gym", listOf(EquipmentSelection("pullup-bar"), EquipmentSelection("mat")))
        assertFalse(isAvailable(mu, gym))
        val withBar = buildProfile("gym", "Gym", listOf(EquipmentSelection("pullup-bar"), EquipmentSelection("high-bar")))
        assertTrue(isAvailable(mu, withBar))
        assertTrue(isAvailable(catalog.variation("pullup-full")!!, withBar))
    }

    @Test fun wallIsACapabilityAndChairIsMarkedStable() {
        val p = buildProfile("t", "T", listOf(EquipmentSelection("wall"), EquipmentSelection("chair")))
        assertTrue("wall" in p.capabilities && EquipmentOptions.FLOOR_SPACE in p.capabilities)
        assertTrue(p.items.none { it.equipmentId == "wall" })
        assertEquals(setOf("stable"), p.items.single { it.equipmentId == "chair" }.suitability)
    }

    @Test fun weightsComeFromTheSelectionOrTheDefault() {
        val p = buildProfile("w", "W", listOf(EquipmentSelection("kettlebell", 16_000), EquipmentSelection("weight")))
        assertEquals(16_000, p.items.single { it.equipmentId == "kettlebell" }.massGrams)
        val dumbbells = p.items.single { it.equipmentId == "weight" }
        assertEquals(2_500, dumbbells.massGrams); assertEquals(2, dumbbells.quantity)
    }

    @Test fun listRules() {
        val seeds = SeedProfiles.all
        assertEquals("gym", ProfileOps.newId("Gym", seeds))
        assertEquals("home-2", ProfileOps.newId("Home", seeds))
        assertEquals("profile", ProfileOps.newId("???", seeds))
        val gym = buildProfile("gym", "Gym", listOf(EquipmentSelection("pullup-bar")))
        val three = ProfileOps.upsert(seeds, gym)
        assertEquals(listOf("home", "travel", "gym"), three.map { it.id })
        assertEquals(listOf("home", "travel", "gym"), ProfileOps.upsert(three, gym.copy(name = "Big gym")).map { it.id })
        // deleting the selected profile selects the first remaining; the last profile cannot be deleted
        val ok = ProfileOps.delete(three, "gym", "gym") as ProfileEditResult.Ok
        assertEquals(listOf("home", "travel"), ok.profiles.map { it.id }); assertEquals("home", ok.selectedId)
        assertEquals("travel", (ProfileOps.delete(three, "gym", "travel") as ProfileEditResult.Ok).selectedId)
        assertTrue(ProfileOps.delete(listOf(gym), "gym", "gym") is ProfileEditResult.Refused)
        assertTrue(ProfileOps.delete(three, "nope", "home") is ProfileEditResult.Refused)
    }

    @Test fun nameValidation() {
        val seeds = SeedProfiles.all
        assertEquals(NameProblem.EMPTY, ProfileOps.nameProblem("  ", seeds, null))
        assertEquals(NameProblem.DUPLICATE, ProfileOps.nameProblem("home", seeds, null))
        assertNull(ProfileOps.nameProblem("Home", seeds, "home")) // renaming to its own name is fine
        assertNull(ProfileOps.nameProblem("Gym", seeds, null))
    }

    @Test fun profilesSurviveABackupRoundTripAndOldFilesStillImport() {
        val gym = buildProfile("gym", "Gym", listOf(EquipmentSelection("high-bar"), EquipmentSelection("kettlebell", 20_000)))
        val payload = app.calisthenics.domain.backup.BackupPayload(app.calisthenics.domain.model.Preferences(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), listOf(gym))
        val back = (app.calisthenics.domain.backup.importBackup(app.calisthenics.domain.backup.exportBackup(payload, "t", 0L)) as app.calisthenics.domain.backup.ImportResult.Ok).payload
        assertEquals(listOf(gym), back.profiles)
        // a payload written before profiles existed has no "profiles" key
        val old = kotlinx.serialization.json.Json { encodeDefaults = false }.encodeToString(app.calisthenics.domain.backup.BackupPayload.serializer(), payload.copy(profiles = emptyList()))
        assertTrue(!old.contains("profiles"))
        assertEquals(emptyList<app.calisthenics.domain.model.EquipmentProfile>(), kotlinx.serialization.json.Json.decodeFromString(app.calisthenics.domain.backup.BackupPayload.serializer(), old).profiles)
    }
}
