package org.blueskiron.goldilocks.api;

/**
 * @author jurajzachar
 *
 */
public abstract class Member {

  private final String binding;

  public Member(String binding) {
    this.binding = binding;
  }

  public String getId() {
    return binding;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((binding == null) ? 0 : binding.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Member))
      return false;
    Member other = (Member) obj;
    if (binding == null) {
      if (other.binding != null)
        return false;
    } else if (!binding.equals(other.binding))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format("node: %s", getId());
  }

}
