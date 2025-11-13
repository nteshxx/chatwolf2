package eureka

import (
	"context"
	"fmt"
	"time"

	"github.com/hudl/fargo"
	"github.com/nteshxx/chatwolf2/socket/internal/observability/logger"
)

type Client struct {
	conn     *fargo.EurekaConnection
	instance *fargo.Instance
	log      *logger.Logger
	cancel   context.CancelFunc
}

func NewClient(ctx context.Context, eurekaURL, appName, host string, port int, log *logger.Logger) (*Client, error) {
	conn := fargo.NewConn(eurekaURL)

	instance := &fargo.Instance{
		App:            appName,
		HostName:       host,
		IPAddr:         host,
		VipAddress:     appName,
		Port:           port,
		Status:         fargo.UP,
		StatusPageUrl:  fmt.Sprintf("http://%s:%d/health", host, port),
		HealthCheckUrl: fmt.Sprintf("http://%s:%d/health", host, port),
		DataCenterInfo: fargo.DataCenterInfo{
			Class: "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
			Name:  "MyOwn",
		},
	}

	instance.SetMetadataString("ws-enabled", "true")
	instance.SetMetadataString("ws-path", "/socket/connect")

	if err := conn.RegisterInstance(instance); err != nil {
		return nil, fmt.Errorf("failed to register with eureka: %w", err)
	}

	log.Info(ctx, "registered with eureka", map[string]interface{}{
		"app":  appName,
		"host": host,
		"port": port,
	})

	ctx, cancel := context.WithCancel(ctx)
	client := &Client{
		conn:     &conn,
		instance: instance,
		log:      log,
		cancel:   cancel,
	}

	go client.heartbeat(ctx)

	return client, nil
}

func (c *Client) heartbeat(ctx context.Context) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := c.conn.HeartBeatInstance(c.instance); err != nil {
				c.log.Error(ctx, "failed to send heartbeat", err)
			}
		}
	}
}

func (c *Client) Deregister(ctx context.Context) error {
	c.cancel()

	if err := c.conn.DeregisterInstance(c.instance); err != nil {
		c.log.Error(ctx, "failed to deregister from eureka", err)
		return err
	}

	c.log.Info(ctx, "deregistered from eureka")
	return nil
}
