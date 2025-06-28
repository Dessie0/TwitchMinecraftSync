package com.twitchmcsync.twitchminecraft.storage;

import com.twitchmcsync.twitchminecraft.TwitchMinecraft;
import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.twitchapi.twitch.OAuthToken;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class DatabaseManager {

    private final String url;
    private final String username;
    private final String password;


    @SneakyThrows
    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        this.username = username;
        this.password = password;

        this.initTable();
        TwitchMinecraft.getInstance().getLogger().info("Successfully connected to database.");
    }

    private void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS twitch_players (
                uuid VARCHAR(36) PRIMARY KEY,
                channel_id VARCHAR(100) NOT NULL,
                channel_name VARCHAR(100) NOT NULL,
                access_token VARCHAR(100) NOT NULL,
                refresh_token VARCHAR(100) NOT NULL,
                tier INT NOT NULL,
                subbed BOOL NOT NULL
            )
        """;

        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try(Connection connection = this.createConnection(); Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void deletePlayer(TwitchPlayer player) {
        String sql = "DELETE FROM twitch_players WHERE uuid = ?";

        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try (Connection connection = this.createConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, player.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void savePlayer(TwitchPlayer player) {
        String sql = """
            INSERT INTO twitch_players (uuid, channel_id, channel_name, access_token, refresh_token, tier, subbed)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                channel_id = VALUES(channel_id),
                channel_name = VALUES(channel_name),
                access_token = VALUES(access_token),
                refresh_token = VALUES(refresh_token),
                tier = VALUES(tier),
                subbed = VALUES(subbed)
        """;

        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try (Connection connection = this.createConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, player.getUuid().toString());
                ps.setString(2, player.getChannelID());
                ps.setString(3, player.getChannelName());
                ps.setString(4, player.getToken().getAccessToken());
                ps.setString(5, player.getToken().getRefreshToken());
                ps.setInt(6, player.getTier());
                ps.setBoolean(7, player.isSubbed());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<TwitchPlayer> loadFromChannelName(String channelId) {
        String sql = "SELECT * FROM twitch_players WHERE channel_name = ?";

        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try (Connection connection = this.createConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, channelId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String channelID = rs.getString("channel_id");
                    String channelName = rs.getString("channel_name");
                    OAuthToken token = new OAuthToken(rs.getString("access_token"), rs.getString("refresh_token"));
                    int tier = rs.getInt("tier");
                    boolean subbed = rs.getBoolean("subbed");
                    future.complete(new TwitchPlayer(TwitchMinecraft.getInstance(), uuid, channelID, channelName, token, tier, subbed));
                }

                future.complete(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return future;
    }

    public CompletableFuture<TwitchPlayer> loadFromChannelId(String channelId) {
        String sql = "SELECT * FROM twitch_players WHERE channel_id = ?";

        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try (Connection connection = this.createConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, channelId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String channelID = rs.getString("channel_id");
                    String channelName = rs.getString("channel_name");
                    OAuthToken token = new OAuthToken(rs.getString("access_token"), rs.getString("refresh_token"));
                    int tier = rs.getInt("tier");
                    boolean subbed = rs.getBoolean("subbed");
                    future.complete(new TwitchPlayer(TwitchMinecraft.getInstance(), uuid, channelID, channelName, token, tier, subbed));
                }

                future.complete(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return future;
    }

    public CompletableFuture<TwitchPlayer> loadPlayer(UUID uuid) {
        String sql = "SELECT * FROM twitch_players WHERE uuid = ?";

        CompletableFuture<TwitchPlayer> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(TwitchMinecraft.getInstance(), () -> {
            try (Connection connection = this.createConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String channelID = rs.getString("channel_id");
                    String channelName = rs.getString("channel_name");
                    OAuthToken token = new OAuthToken(rs.getString("access_token"), rs.getString("refresh_token"));
                    int tier = rs.getInt("tier");
                    boolean subbed = rs.getBoolean("subbed");
                    future.complete(new TwitchPlayer(TwitchMinecraft.getInstance(), uuid, channelID, channelName, token, tier, subbed));
                }

                future.complete(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return future;
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(this.getUrl(), this.getUsername(), this.getPassword());
    }
}
