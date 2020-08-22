package com.being0.stockprice;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="mailto:raliakbari@gmail.com">Reza Aliakbari</a>
 * @version 1, 08/20/2020
 */
@Data
@AllArgsConstructor
public class PriceResult {

    private String stock;
    // Spot price
    private Double spot;
    // Daily average price
    private Double daily;
}
