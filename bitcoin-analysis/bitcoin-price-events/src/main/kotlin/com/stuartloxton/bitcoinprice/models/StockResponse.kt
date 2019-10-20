package com.stuartloxton.bitcoinprice.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory

class StockResponse {
    private val log = LoggerFactory.getLogger(StockResponse::class.java)

    @JsonProperty(value = "error")
    var error: String? = null

    @JsonProperty(value = "errorDescription")
    var errorDescription: MutableList<String?> = mutableListOf()

    @JsonProperty(value = "data")
    var data: String? = null

    constructor(error: String?, errorDescription: MutableList<String?>, data: String?) {
        this.error = error
        this.errorDescription = errorDescription
        this.data = data
    }

    override fun toString(): String {
        return "ModifiedAddressResponse(error=$error, errorDescription=$errorDescription, data=$data)"
    }
}