<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" standalone="no"/>

    <xsl:template match="/cafe_order">
        <cafe_order>
            <xsl:copy-of select="order_id"/>
            
            <drinks>
                <xsl:for-each select="drinks/drink">
                    <drink>
                        <xsl:copy-of select="name"/>
                        <xsl:copy-of select="type"/>
                        
                        <!-- STOCK: Extraemos el valor numérico de la BD -->
                        <stock>
                            <xsl:choose>
                                <xsl:when test="fila/stock">
                                    <xsl:value-of select="fila/stock"/>
                                </xsl:when>
                                <xsl:otherwise>0</xsl:otherwise>
                            </xsl:choose>
                        </stock>
                    </drink>
                </xsl:for-each>
            </drinks>

            <!-- TOTAL: Sumamos los precios de la BD -->
            <total>
                <cobrado>
                    <!-- 'sum' suma todos los nodos precio encontrados en las filas -->
                    <xsl:value-of select="sum(drinks/drink/fila/precio)"/>
                </cobrado>
            </total>

        </cafe_order>
    </xsl:template>
</xsl:stylesheet>