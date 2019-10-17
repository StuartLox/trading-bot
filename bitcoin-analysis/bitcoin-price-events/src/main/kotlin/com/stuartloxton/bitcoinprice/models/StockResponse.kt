package com.stuartloxton.bitcoinprice.models

import com.fasterxml.jackson.annotation.JsonProperty

class StockResponse {
    @JsonProperty(value = "error")
    var error: String? = null

    @JsonProperty(value = "errorDescription")
    var errorDescription: MutableList<String?> = mutableListOf()

    @JsonProperty(value = "data")
    var data: ArrayList<Stock>? = null

    constructor(error: String?, errorDescription: MutableList<String?>, data: ArrayList<Stock>?) {
        this.error = error
        this.errorDescription = errorDescription
        this.data = data
    }

    override fun toString(): String {
        return "ModifiedAddressResponse(error=$error, errorDescription=$errorDescription, data=$data)"
    }
}