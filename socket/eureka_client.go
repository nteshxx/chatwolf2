package main

import (
    "bytes"
    "context"
    "encoding/xml"
    "fmt"
    "net/http"
    "net/url"
    "time"
)

type EurekaInstance struct {
    XMLName xml.Name `xml:"instance"`
    HostName string `xml:"hostName"`
    App string `xml:"app"`
    IpAddr string `xml:"ipAddr"`
    Port struct {
        Port int `xml:",chardata"`
        Enabled bool `xml:"enabled,attr"`
    } `xml:"port"`
    VipAddress string `xml:"vipAddress"`
    Status string `xml:"status"`
    DataCenterInfo struct {
        Class string `xml:"class,attr"`
        Name string `xml:"name"`
    } `xml:"dataCenterInfo"`
}

func RegisterWithEureka(eurekaURL, appName, host string, port int) (func(), error) {
    inst := EurekaInstance{
        HostName: host,
        App: appName,
        IpAddr: host,
        Status: "UP",
    }
    inst.Port.Port = port
    inst.Port.Enabled = true
    inst.VipAddress = appName
    inst.DataCenterInfo.Class = "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo"
    inst.DataCenterInfo.Name = "MyOwn"

    body := struct {
        XMLName xml.Name `xml:"application"`
        Instance EurekaInstance `xml:"instance"`
    }{Instance: inst}

    b, _ := xml.Marshal(body)
    urlStr := fmt.Sprintf("%s/apps/%s", eurekaURL, appName)
    req, _ := http.NewRequest("POST", urlStr, bytes.NewReader(b))
    req.Header.Set("Content-Type", "application/xml")
    client := &http.Client{Timeout: 5 * time.Second}
    resp, err := client.Do(req)
    if err != nil {
        return nil, err
    }
    resp.Body.Close()

    done := make(chan struct{})
    go func() {
        ticker := time.NewTicker(30 * time.Second)
        for {
            select {
            case <-ticker.C:
                heartbeatURL := fmt.Sprintf("%s/apps/%s/%s:%d", eurekaURL, appName, host, port)
                client.Put(heartbeatURL, "application/xml", nil)
            case <-done:
                return
            }
        }
    }()

    dereg := func() {
        close(done)
        delURL := fmt.Sprintf("%s/apps/%s/%s:%d", eurekaURL, appName, host, port)
        req, _ := http.NewRequest("DELETE", delURL, nil)
        client.Do(req)
    }
    return dereg, nil
}
