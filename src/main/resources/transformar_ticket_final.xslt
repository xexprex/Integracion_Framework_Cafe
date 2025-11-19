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
        </cafe_order>
    </xsl:template>
</xsl:stylesheet>