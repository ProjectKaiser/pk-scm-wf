package org.scm4j.releaser.conf;

import org.scm4j.commons.Version;

public class DelayedTag {
	private final Version version;
	private final String revision;

	public DelayedTag(Version version, String revision) {
		this.version = version;
		this.revision = revision;
	}

	public Version getVersion() {
		return version;
	}

	public String getRevision() {
		return revision;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DelayedTag that = (DelayedTag) o;

		if (version != null ? !version.equals(that.version) : that.version != null) return false;
		return !(revision != null ? !revision.equals(that.revision) : that.revision != null);

	}

	@Override
	public int hashCode() {
		int result = version != null ? version.hashCode() : 0;
		result = 31 * result + (revision != null ? revision.hashCode() : 0);
		return result;
	}
}
