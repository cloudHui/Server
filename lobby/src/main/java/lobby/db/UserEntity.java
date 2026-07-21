package lobby.db;

/**
 * SQLite user 表实体
 */
public class UserEntity {
	private long id;
	private String username;
	private String nickname;
	private String passwordHash;
	private boolean enabled;
	private String token;
	private long createdAt;
	private Long lastLoginAt;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public Long getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(Long lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}
}
