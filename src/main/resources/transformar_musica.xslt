<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <xsl:template match="/pedido/info_original">
        <producto_musical>
            <banda><xsl:value-of select="artista"/></banda>
            <titulo><xsl:value-of select="album"/></titulo>
            <origen>Transformado por XSLT</origen>
        </producto_musical>
    </xsl:template>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
