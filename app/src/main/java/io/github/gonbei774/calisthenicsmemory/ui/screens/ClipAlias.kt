// R7-T12: until the clip track delivers a clip for a new exercise, the app shows the closest existing one (declared in
// tools/gen_demo_clips_v3.py SHARED_POSES). Assets in demos/ and thumbs/ change only through the clip track, so this is done here.
package io.github.gonbei774.calisthenicsmemory.ui.screens

object ClipAlias {
    private val closest = mapOf("jefferson-curl" to "rdl-bodyweight", "german-hang" to "dead-hang")
    /** The asset id to use for [id]: its own when it has a clip, else the closest existing exercise's. */
    fun assetId(id: String, hasOwn: (String) -> Boolean): String = if (hasOwn(id)) id else closest[id] ?: id
}
