package eu.domibus.common.model.configuration;

import eu.domibus.common.model.common.RevisionLogicalName;
import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;


/**
 * <p>Java class for anonymous complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="identifier" maxOccurs="unbounded">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="partyId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="partyIdType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="userName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="password" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="endpoint" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Christian Koch, Stefan Mueller
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = "identifiers")
@Entity
@Table(name = "TB_PARTY")
@NamedQueries({@NamedQuery(name = "Party.findPartyByIdentifier", query = "select p.name from Party p where :PARTY_IDENTIFIER member of p.identifiers"),
        @NamedQuery(name = "Party.findByName", query = "select p from Party p where p.name = :NAME"),
        @NamedQuery(name = "Party.findPartyIdentifiersByEndpoint", query = "select p.identifiers from Party p where p.endpoint = :ENDPOINT")})
@Audited(withModifiedFlag = true)
@RevisionLogicalName("Party")
public class Party extends AbstractBaseEntity {

    @XmlElement(required = true, name = "identifier")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_PARTY")
    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "TB_PARTY_IDENTIFIER_AUD")
    protected Set<Identifier> identifiers; //NOSONAR
    @XmlAttribute(name = "name", required = true)
    @Column(name = "NAME")
    protected String name;
    @XmlAttribute(name = "userName")
    @Column(name = "USERNAME")
    protected String userName;
    @XmlAttribute(name = "password")
    @Column(name = "PASSWORD")//TODO:HASH!
    protected String password;
    @XmlAttribute(name = "endpoint", required = true)
    @XmlSchemaType(name = "anyURI")
    @Column(name = "ENDPOINT")
    protected String endpoint;

    /**
     * Gets the value of the identifier property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the identifier property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIdentifier().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Identifier }
     */
    public Set<Identifier> getIdentifiers() {
        if (this.identifiers == null) {
            this.identifiers = new HashSet<>();
        }
        return this.identifiers;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(final String value) {
        this.name = value;
    }

    /**
     * Gets the value of the userName property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets the value of the userName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUserName(final String value) {
        this.userName = value;
    }

    /**
     * Gets the value of the password property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the value of the password property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPassword(final String value) {
        this.password = value;
    }

    /**
     * Gets the value of the endpoint property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Sets the value of the endpoint property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEndpoint(final String value) {
        this.endpoint = value;
    }


    public void init(final Configuration configuration) {
        for (final Identifier identifier : this.identifiers) {
            identifier.init(configuration);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Party party = (Party) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(identifiers, party.identifiers)
                .append(name, party.name)
                .append(endpoint, party.endpoint)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(identifiers)
                .append(name)
                .append(endpoint)
                .toHashCode();
    }
}
