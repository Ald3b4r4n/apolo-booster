package br.com.apolo.booster.audio

/**
 * O que o sistema realmente respondeu ao tentar aplicar cada efeito.
 *
 * Criar um AudioEffect com sucesso NAO significa que ele foi inserido no mix
 * de saida. Estes campos separam "criado" de "controlando e habilitado".
 */
data class BoostTelemetry(
    val loudnessCreated: Boolean = false,
    val loudnessControl: Boolean = false,
    val loudnessEnabled: Boolean = false,
    val loudnessTargetMb: Int = 0,
    val loudnessSetStatus: Int = Int.MIN_VALUE,
    val equalizerControl: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val bassControl: Boolean = false,
    val bassEnabled: Boolean = false,
    val bassRoundedStrength: Int? = null,
    val bassStrengthSupported: Boolean? = null,
    val musicActive: Boolean = false,
    val outputRoute: String = "-",
    val reattachCount: Int = 0,
    val lastEvent: String = "—",
)
