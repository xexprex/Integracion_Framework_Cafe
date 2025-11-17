<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <xsl:template match="/cafe_order | /">
        
        <xsl:variable name="primerPrecio" select="normalize-space(//fila/precio[1])" />
        
        <xsl:variable name="montoEnCentimos">
            <xsl:choose>
                <xsl:when test="string($primerPrecio)">
                    <xsl:value-of select="$primerPrecio * 100" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="0" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <peticionPago>
            <monto><xsl:value-of select="format-number($montoEnCentimos, '0')"/></monto>
            <moneda>eur</moneda>
            <fuente>tok_visa</fuente>
        </peticionPago>
    </xsl:template>

    <xsl:template match="text()"/>
</xsl:stylesheet>