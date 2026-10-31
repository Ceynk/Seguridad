package com.antiphishing.smishguard.domain.detector

enum class ReputacionDominio { MALICIOSO, DESCONOCIDO, CONFIABLE }

interface DomainReputationSource {
    fun consultar(dominio: String): ReputacionDominio
}

object SinReputacionExterna : DomainReputationSource {
    override fun consultar(dominio: String): ReputacionDominio = ReputacionDominio.DESCONOCIDO
}
