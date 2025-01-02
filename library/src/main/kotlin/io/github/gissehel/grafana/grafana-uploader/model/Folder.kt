package io.github.gissehel.grafana.`grafana-uploader`.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val uid: String,
    val title: String,
    val parentUid: String
)
