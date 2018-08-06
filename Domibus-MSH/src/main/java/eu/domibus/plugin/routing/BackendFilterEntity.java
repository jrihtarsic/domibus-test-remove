package eu.domibus.plugin.routing;

import eu.domibus.common.model.common.RevisionLogicalName;
import eu.domibus.ebms3.common.model.AbstractBaseEntity;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Walczac
 */
@Entity
@Table(name = "TB_BACKEND_FILTER")
@NamedQueries({
        @NamedQuery(name = "BackendFilter.findEntries", query = "select bf from BackendFilterEntity bf order by bf.index")
})
@Audited(withModifiedFlag = true)
@RevisionLogicalName("Message filter")
public class BackendFilterEntity extends AbstractBaseEntity implements Comparable<BackendFilterEntity> {

    @Column(name = "PRIORITY")
    private int index;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "FK_BACKEND_FILTER")
    @OrderColumn(name = "PRIORITY")
    @AuditJoinTable(name = "TB_BACK_RCRITERIA_AUD")
    private List<RoutingCriteriaEntity> routingCriterias = new ArrayList<>();

    @Column(name = "BACKEND_NAME")
    private String backendName;
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Gets the value of the routingCriteria property.
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the BackendFilterEntity object. This is why there is not a
     * <CODE>set</CODE> method for the property.
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRoutingCriteriaEntities().add(newItem);
     * </pre>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RoutingCriteriaEntity }
     *
     * @return a reference to the live list of routingCriteria
     */
    public List<RoutingCriteriaEntity> getRoutingCriterias() {
        return routingCriterias;
    }

    public void setRoutingCriterias(List<RoutingCriteriaEntity> routingCriterias) {
        this.routingCriterias = routingCriterias;
    }

    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(final String backendName) {
        this.backendName = backendName;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p>
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     * <p>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     * <p>
     * Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     * <p>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     * <p>
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(final BackendFilterEntity o) {
        return index - o.index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BackendFilterEntity that = (BackendFilterEntity) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(index, that.index)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(index)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("index", index)
                .append("routingCriterias", routingCriterias)
                .append("backendName", backendName)
                .toString();
    }
}
