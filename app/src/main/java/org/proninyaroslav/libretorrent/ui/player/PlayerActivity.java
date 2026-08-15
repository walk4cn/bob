/*
 * Copyright (C) 2026
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.proninyaroslav.libretorrent.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.google.android.material.snackbar.Snackbar;

import org.proninyaroslav.libretorrent.R;
import org.proninyaroslav.libretorrent.databinding.ActivityPlayerBinding;

/*
 * The built-in player for progressive playback (streaming) of a file from a torrent.
 * Receives the URL of the local streaming server (TorrentStreamServer)
 * and plays it while the torrent is being downloaded.
 */

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = PlayerActivity.class.getSimpleName();

    private static final String EXTRA_URL = "url";
    private static final String EXTRA_TITLE = "title";

    private static final String TAG_PLAY_WHEN_READY = "play_when_ready";
    private static final String TAG_PLAYBACK_POSITION = "playback_position";

    /* The torrent piece may not be downloaded yet, so the request may take a long time */
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private ActivityPlayerBinding binding;
    private PlayerView playerView;
    private ExoPlayer player;
    private boolean playWhenReady = true;
    private long playbackPosition = 0L;

    public static Intent newIntent(@NonNull Context context,
                                   @NonNull String url,
                                   @NonNull String title) {
        return new Intent(context, PlayerActivity.class)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        playerView = binding.playerView;

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null)
            setTitle(title);

        if (savedInstanceState != null) {
            playWhenReady = savedInstanceState.getBoolean(TAG_PLAY_WHEN_READY, true);
            playbackPosition = savedInstanceState.getLong(TAG_PLAYBACK_POSITION, 0L);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initializePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        releasePlayer();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (player != null) {
            outState.putBoolean(TAG_PLAY_WHEN_READY, player.getPlayWhenReady());
            outState.putLong(TAG_PLAYBACK_POSITION, player.getCurrentPosition());
        } else {
            outState.putBoolean(TAG_PLAY_WHEN_READY, playWhenReady);
            outState.putLong(TAG_PLAYBACK_POSITION, playbackPosition);
        }
    }

    private void initializePlayer() {
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            showError(getString(R.string.player_error));
            finish();

            return;
        }

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();

        playerView.setPlayer(player);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(playbackPosition);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                binding.loading.setVisibility(
                        playbackState == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE
                );
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                binding.loading.setVisibility(View.GONE);
                showError(getString(R.string.player_error));
            }
        });

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(url)
                .setTag(getIntent().getStringExtra(EXTRA_TITLE))
                .build();
        player.setMediaItem(mediaItem);
        player.prepare();
    }

    private void releasePlayer() {
        if (player == null)
            return;

        playWhenReady = player.getPlayWhenReady();
        playbackPosition = player.getCurrentPosition();
        playerView.setPlayer(null);
        player.release();
        player = null;
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}