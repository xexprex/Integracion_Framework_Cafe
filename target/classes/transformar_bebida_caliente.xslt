<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes"/>

    <xsl:template match="/drink">
        <peticion>
            <sql>
                <xsl:text>SELECT * FROM bebidas WHERE nombre = '</xsl:text>
                <xsl:value-of select="name"/>
                <xsl:text>'</xsl:text>
            </sql>
        </peticion>
    </xsl:template>
</xsl:stylesheet>